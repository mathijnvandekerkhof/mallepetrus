package nl.mallepetrus.jiptv.service;

import nl.mallepetrus.jiptv.dto.DeviceFingerprintRequest;
import nl.mallepetrus.jiptv.dto.RiskAssessmentResponse;
import nl.mallepetrus.jiptv.entity.*;
import nl.mallepetrus.jiptv.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ZeroTrustService {

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${jiptv.zero-trust.enabled:true}")
    private boolean zeroTrustEnabled;

    @Value("${jiptv.zero-trust.risk-threshold:75}")
    private int riskThreshold;

    @Value("${jiptv.zero-trust.max-devices:5}")
    private int maxDevicesPerUser;

    private static final String REDIS_KEY_PREFIX = "zt:";
    private static final String FAILED_ATTEMPTS_KEY = "failed_attempts:";
    private static final String RATE_LIMIT_KEY = "rate_limit:";

    @Transactional
    public DeviceFingerprint processDeviceFingerprint(User user, DeviceFingerprintRequest request, 
                                                     HttpServletRequest httpRequest) {
        if (!zeroTrustEnabled) {
            return null;
        }

        String fingerprintHash = generateDeviceFingerprint(request, httpRequest);
        
        Optional<DeviceFingerprint> existingDevice = deviceFingerprintRepository.findByFingerprintHash(fingerprintHash);
        
        if (existingDevice.isPresent()) {
            // Update existing device
            DeviceFingerprint device = existingDevice.get();
            device.setLastSeenAt(LocalDateTime.now());
            device.setAccessCount(device.getAccessCount() + 1);
            
            // Update IP address if changed
            try {
                InetAddress currentIp = InetAddress.getByName(getClientIpAddress(httpRequest));
                device.setIpAddress(currentIp);
            } catch (UnknownHostException e) {
                // Log error but continue
            }
            
            return deviceFingerprintRepository.save(device);
        } else {
            // Create new device
            DeviceFingerprint newDevice = new DeviceFingerprint(user, fingerprintHash);
            newDevice.setDeviceName(request.getDeviceName());
            newDevice.setUserAgent(request.getUserAgent());
            newDevice.setScreenResolution(request.getScreenResolution());
            newDevice.setTimezone(request.getTimezone());
            newDevice.setLanguage(request.getLanguage());
            newDevice.setPlatform(request.getPlatform());
            
            try {
                newDevice.setIpAddress(InetAddress.getByName(getClientIpAddress(httpRequest)));
            } catch (UnknownHostException e) {
                // Log error but continue
            }
            
            // Log new device event
            logSecurityEvent(user, newDevice, SecurityEvent.SecurityEventType.NEW_DEVICE_DETECTED, 25);
            
            return deviceFingerprintRepository.save(newDevice);
        }
    }

    @Transactional
    public RiskAssessmentResponse assessRisk(User user, DeviceFingerprint device, HttpServletRequest request) {
        if (!zeroTrustEnabled) {
            return new RiskAssessmentResponse(0, "LOW", false);
        }

        List<RiskAssessmentResponse.RiskFactor> riskFactors = new ArrayList<>();
        int totalRiskScore = 0;

        // Check for new device
        if (device != null && !device.isTrusted()) {
            riskFactors.add(new RiskAssessmentResponse.RiskFactor(
                "NEW_DEVICE", "Unrecognized device", 25, "MEDIUM"));
            totalRiskScore += 25;
        }

        // Check for unusual time access
        LocalTime currentTime = LocalTime.now();
        if (currentTime.isBefore(LocalTime.of(6, 0)) || currentTime.isAfter(LocalTime.of(23, 0))) {
            riskFactors.add(new RiskAssessmentResponse.RiskFactor(
                "UNUSUAL_TIME", "Access outside normal hours", 15, "LOW"));
            totalRiskScore += 15;
        }

        // Check for multiple failed attempts
        String clientIp = getClientIpAddress(request);
        String failedAttemptsKey = REDIS_KEY_PREFIX + FAILED_ATTEMPTS_KEY + clientIp;
        Integer failedAttempts = (Integer) redisTemplate.opsForValue().get(failedAttemptsKey);
        if (failedAttempts != null && failedAttempts >= 3) {
            riskFactors.add(new RiskAssessmentResponse.RiskFactor(
                "MULTIPLE_FAILURES", "Multiple failed login attempts", 35, "HIGH"));
            totalRiskScore += 35;
        }

        // Check for concurrent sessions
        long activeSessions = userSessionRepository.countActiveSessionsForUser(user);
        if (activeSessions >= maxDevicesPerUser) {
            riskFactors.add(new RiskAssessmentResponse.RiskFactor(
                "CONCURRENT_SESSIONS", "Too many active sessions", 20, "MEDIUM"));
            totalRiskScore += 20;
        }

        // Check recent security events
        LocalDateTime recentThreshold = LocalDateTime.now().minusHours(24);
        long recentHighRiskEvents = securityEventRepository.findHighRiskEvents(user, 30, recentThreshold).size();
        if (recentHighRiskEvents > 0) {
            riskFactors.add(new RiskAssessmentResponse.RiskFactor(
                "RECENT_INCIDENTS", "Recent high-risk security events", 30, "HIGH"));
            totalRiskScore += 30;
        }

        // Determine risk level and recommendations
        String riskLevel = determineRiskLevel(totalRiskScore);
        boolean requiresStepUp = totalRiskScore >= riskThreshold;
        String recommendation = generateRecommendation(totalRiskScore, riskLevel);

        RiskAssessmentResponse response = new RiskAssessmentResponse(totalRiskScore, riskLevel, requiresStepUp);
        response.setRiskFactors(riskFactors);
        response.setRecommendation(recommendation);

        return response;
    }

    @Transactional
    public void logSecurityEvent(User user, DeviceFingerprint device, 
                                SecurityEvent.SecurityEventType eventType, Integer riskScore) {
        SecurityEvent event = new SecurityEvent(user, eventType, riskScore);
        event.setDeviceFingerprint(device);
        
        securityEventRepository.save(event);
    }

    @Transactional
    public void recordFailedAttempt(String clientIp) {
        String key = REDIS_KEY_PREFIX + FAILED_ATTEMPTS_KEY + clientIp;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 15, TimeUnit.MINUTES); // Reset after 15 minutes
    }

    @Transactional
    public void clearFailedAttempts(String clientIp) {
        String key = REDIS_KEY_PREFIX + FAILED_ATTEMPTS_KEY + clientIp;
        redisTemplate.delete(key);
    }

    @Transactional
    public UserSession createSession(User user, DeviceFingerprint device, String sessionToken, 
                                   RiskAssessmentResponse riskAssessment, HttpServletRequest request) {
        UserSession session = new UserSession(user, sessionToken, LocalDateTime.now().plusDays(1));
        session.setDeviceFingerprint(device);
        session.setInitialRiskScore(riskAssessment.getCurrentRiskScore());
        session.setCurrentRiskScore(riskAssessment.getCurrentRiskScore());
        session.setRequiresStepUpAuth(riskAssessment.isRequiresStepUpAuth());
        
        try {
            session.setIpAddress(InetAddress.getByName(getClientIpAddress(request)));
        } catch (UnknownHostException e) {
            // Log error but continue
        }
        
        return userSessionRepository.save(session);
    }

    public boolean isRateLimited(String clientIp) {
        String key = REDIS_KEY_PREFIX + RATE_LIMIT_KEY + clientIp;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
        
        if (attempts == null) {
            redisTemplate.opsForValue().set(key, 1, 1, TimeUnit.MINUTES);
            return false;
        }
        
        if (attempts >= 10) { // Max 10 requests per minute
            return true;
        }
        
        redisTemplate.opsForValue().increment(key);
        return false;
    }

    @Transactional
    public void trustDevice(User user, Long deviceId) {
        Optional<DeviceFingerprint> device = deviceFingerprintRepository.findById(deviceId);
        if (device.isPresent() && device.get().getUser().equals(user)) {
            device.get().setTrusted(true);
            deviceFingerprintRepository.save(device.get());
            
            logSecurityEvent(user, device.get(), SecurityEvent.SecurityEventType.DEVICE_TRUSTED, 0);
        }
    }

    @Transactional
    public void untrustDevice(User user, Long deviceId) {
        Optional<DeviceFingerprint> device = deviceFingerprintRepository.findById(deviceId);
        if (device.isPresent() && device.get().getUser().equals(user)) {
            device.get().setTrusted(false);
            deviceFingerprintRepository.save(device.get());
            
            logSecurityEvent(user, device.get(), SecurityEvent.SecurityEventType.DEVICE_UNTRUSTED, 10);
        }
    }

    private String generateDeviceFingerprint(DeviceFingerprintRequest request, HttpServletRequest httpRequest) {
        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(request.getUserAgent() != null ? request.getUserAgent() : "");
        fingerprint.append(request.getScreenResolution() != null ? request.getScreenResolution() : "");
        fingerprint.append(request.getTimezone() != null ? request.getTimezone() : "");
        fingerprint.append(request.getLanguage() != null ? request.getLanguage() : "");
        fingerprint.append(request.getPlatform() != null ? request.getPlatform() : "");
        
        // Add some headers for additional uniqueness
        fingerprint.append(httpRequest.getHeader("Accept-Language") != null ? 
                          httpRequest.getHeader("Accept-Language") : "");
        fingerprint.append(httpRequest.getHeader("Accept-Encoding") != null ? 
                          httpRequest.getHeader("Accept-Encoding") : "");
        
        return hashString(fingerprint.toString());
    }

    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String determineRiskLevel(int riskScore) {
        if (riskScore >= 80) return "CRITICAL";
        if (riskScore >= 60) return "HIGH";
        if (riskScore >= 30) return "MEDIUM";
        return "LOW";
    }

    private String generateRecommendation(int riskScore, String riskLevel) {
        switch (riskLevel) {
            case "CRITICAL":
                return "Access denied. Multiple security concerns detected. Contact administrator.";
            case "HIGH":
                return "Additional authentication required. Verify identity through MFA.";
            case "MEDIUM":
                return "Proceed with caution. Monitor session for unusual activity.";
            default:
                return "Access granted. Normal security posture.";
        }
    }
}