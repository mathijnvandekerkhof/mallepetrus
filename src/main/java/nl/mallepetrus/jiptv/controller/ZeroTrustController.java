package nl.mallepetrus.jiptv.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.DeviceFingerprintRequest;
import nl.mallepetrus.jiptv.dto.RiskAssessmentResponse;
import nl.mallepetrus.jiptv.entity.DeviceFingerprint;
import nl.mallepetrus.jiptv.entity.SecurityEvent;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.entity.UserSession;
import nl.mallepetrus.jiptv.repository.DeviceFingerprintRepository;
import nl.mallepetrus.jiptv.repository.SecurityEventRepository;
import nl.mallepetrus.jiptv.repository.UserRepository;
import nl.mallepetrus.jiptv.repository.UserSessionRepository;
import nl.mallepetrus.jiptv.service.ZeroTrustService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zero-trust")
public class ZeroTrustController {

    @Autowired
    private ZeroTrustService zeroTrustService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @PostMapping("/assess-risk")
    public ResponseEntity<?> assessRisk(@Valid @RequestBody DeviceFingerprintRequest request,
                                       Authentication authentication,
                                       HttpServletRequest httpRequest) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Process device fingerprint
            DeviceFingerprint device = zeroTrustService.processDeviceFingerprint(user, request, httpRequest);
            
            // Assess risk
            RiskAssessmentResponse riskAssessment = zeroTrustService.assessRisk(user, device, httpRequest);
            
            return ResponseEntity.ok(riskAssessment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Risk assessment failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/devices")
    public ResponseEntity<?> getUserDevices(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<DeviceFingerprint> devices = deviceFingerprintRepository.findByUserOrderByLastSeenAtDesc(user);
            
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get devices: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/devices/{deviceId}/trust")
    public ResponseEntity<?> trustDevice(@PathVariable Long deviceId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            zeroTrustService.trustDevice(user, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device trusted successfully");
            response.put("deviceId", deviceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to trust device: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/devices/{deviceId}/untrust")
    public ResponseEntity<?> untrustDevice(@PathVariable Long deviceId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            zeroTrustService.untrustDevice(user, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device untrusted successfully");
            response.put("deviceId", deviceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to untrust device: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/security-events")
    public ResponseEntity<?> getSecurityEvents(Authentication authentication,
                                              @RequestParam(defaultValue = "7") int days) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<SecurityEvent> events = securityEventRepository.findByUserAndCreatedAtAfterOrderByCreatedAtDesc(user, since);
            
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get security events: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<UserSession> sessions = userSessionRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(user);
            
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get sessions: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/sessions/{sessionId}/terminate")
    public ResponseEntity<?> terminateSession(@PathVariable Long sessionId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Find and terminate the session
            UserSession session = userSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (!session.getUser().equals(user)) {
                throw new RuntimeException("Unauthorized to terminate this session");
            }
            
            session.setActive(false);
            userSessionRepository.save(session);
            
            // Log security event
            zeroTrustService.logSecurityEvent(user, session.getDeviceFingerprint(), 
                                            SecurityEvent.SecurityEventType.SESSION_TERMINATED, 5);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Session terminated successfully");
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to terminate session: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getSecurityDashboard(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Map<String, Object> dashboard = new HashMap<>();
            
            // Device statistics
            List<DeviceFingerprint> allDevices = deviceFingerprintRepository.findByUserOrderByLastSeenAtDesc(user);
            List<DeviceFingerprint> trustedDevices = deviceFingerprintRepository.findByUserAndTrustedTrue(user);
            
            dashboard.put("totalDevices", allDevices.size());
            dashboard.put("trustedDevices", trustedDevices.size());
            dashboard.put("untrustedDevices", allDevices.size() - trustedDevices.size());
            
            // Session statistics
            long activeSessions = userSessionRepository.countActiveSessionsForUser(user);
            dashboard.put("activeSessions", activeSessions);
            
            // Recent security events
            LocalDateTime recentThreshold = LocalDateTime.now().minusDays(7);
            List<SecurityEvent> recentEvents = securityEventRepository.findByUserAndCreatedAtAfterOrderByCreatedAtDesc(user, recentThreshold);
            dashboard.put("recentSecurityEvents", recentEvents.size());
            
            // Risk assessment
            Double avgRiskScore = securityEventRepository.getAverageRiskScoreForUser(user, recentThreshold);
            dashboard.put("averageRiskScore", avgRiskScore != null ? avgRiskScore.intValue() : 0);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get security dashboard: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}