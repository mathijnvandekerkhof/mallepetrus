package nl.mallepetrus.jiptv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.mallepetrus.jiptv.dto.DevicePairingRequest;
import nl.mallepetrus.jiptv.dto.QrCodeResponse;
import nl.mallepetrus.jiptv.dto.TvDeviceResponse;
import nl.mallepetrus.jiptv.entity.DevicePairingToken;
import nl.mallepetrus.jiptv.entity.TvDevice;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.repository.DevicePairingTokenRepository;
import nl.mallepetrus.jiptv.repository.TvDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DevicePairingService {

    private static final Logger logger = LoggerFactory.getLogger(DevicePairingService.class);
    private static final int PAIRING_TOKEN_LENGTH = 64;
    private static final int PAIRING_EXPIRY_MINUTES = 15;
    private static final int MAX_DEVICES_PER_USER = 5;
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Autowired
    private TvDeviceRepository tvDeviceRepository;

    @Autowired
    private DevicePairingTokenRepository pairingTokenRepository;

    @Autowired
    private QrCodeService qrCodeService;

    @Value("${jiptv.domains.api:localhost:8080}")
    private String apiDomain;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate QR code for device pairing
     */
    @Transactional
    public QrCodeResponse generatePairingQrCode(User user) {
        // Check if user has reached device limit
        long activeDevices = tvDeviceRepository.countActiveDevicesByUser(user);
        if (activeDevices >= MAX_DEVICES_PER_USER) {
            throw new IllegalArgumentException("Maximum number of devices reached (" + MAX_DEVICES_PER_USER + ")");
        }

        // Check for existing valid tokens and limit them
        long validTokens = pairingTokenRepository.countValidTokensByUser(user, LocalDateTime.now());
        if (validTokens >= 3) { // Max 3 concurrent pairing attempts
            throw new IllegalArgumentException("Too many pending pairing requests. Please wait for existing tokens to expire.");
        }

        // Generate unique pairing token
        String pairingToken = generatePairingToken();
        while (pairingTokenRepository.findByPairingToken(pairingToken).isPresent()) {
            pairingToken = generatePairingToken();
        }

        // Create QR code data (JSON format)
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("type", "jiptv_pairing");
        qrData.put("version", "1.0");
        qrData.put("token", pairingToken);
        qrData.put("apiUrl", "http://" + apiDomain + "/device-pairing/pair");
        qrData.put("userEmail", user.getEmail());
        qrData.put("expiresAt", LocalDateTime.now().plusMinutes(PAIRING_EXPIRY_MINUTES).toString());

        String qrCodeData;
        try {
            qrCodeData = objectMapper.writeValueAsString(qrData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize QR code data", e);
            throw new RuntimeException("Failed to generate QR code data", e);
        }

        // Generate QR code image
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(qrCodeData);

        // Save pairing token
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PAIRING_EXPIRY_MINUTES);
        DevicePairingToken token = new DevicePairingToken(user, pairingToken, qrCodeData, expiresAt);
        pairingTokenRepository.save(token);

        // Create pairing URL for manual entry
        String pairingUrl = String.format("http://%s/device-pairing/manual?token=%s", apiDomain, pairingToken);

        logger.info("Generated pairing QR code for user: {}, token expires at: {}", user.getEmail(), expiresAt);

        return new QrCodeResponse(pairingToken, qrCodeData, qrCodeBase64, expiresAt, pairingUrl);
    }

    /**
     * Pair TV device using pairing token
     */
    @Transactional
    public TvDeviceResponse pairDevice(DevicePairingRequest request) {
        // Find and validate pairing token
        DevicePairingToken token = pairingTokenRepository.findByPairingToken(request.getPairingToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid pairing token"));

        if (!token.isValid()) {
            throw new IllegalArgumentException("Pairing token is expired or already used");
        }

        // Check if MAC address is already registered
        if (tvDeviceRepository.existsByMacAddress(request.getMacAddress())) {
            throw new IllegalArgumentException("Device with this MAC address is already registered");
        }

        // Hash MAC address for security
        String macAddressHash = hashMacAddress(request.getMacAddress());

        // Create new TV device
        TvDevice tvDevice = new TvDevice(token.getUser(), request.getDeviceName(), 
                                        request.getMacAddress(), macAddressHash);
        tvDevice.setWebosVersion(request.getWebosVersion());
        tvDevice.setModelName(request.getModelName());
        tvDevice.setManufacturer(request.getManufacturer());
        tvDevice.setScreenResolution(request.getScreenResolution());
        tvDevice.updateLastSeen();

        tvDevice = tvDeviceRepository.save(tvDevice);

        // Mark token as used
        token.markAsUsed(tvDevice);
        pairingTokenRepository.save(token);

        logger.info("TV device paired successfully: {} for user: {}", 
                   tvDevice.getDeviceName(), token.getUser().getEmail());

        return mapToTvDeviceResponse(tvDevice);
    }

    /**
     * Get user's TV devices
     */
    public List<TvDeviceResponse> getUserDevices(User user) {
        List<TvDevice> devices = tvDeviceRepository.findByUserOrderByPairedAtDesc(user);
        return devices.stream()
                .map(this::mapToTvDeviceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active TV devices for user
     */
    public List<TvDeviceResponse> getActiveUserDevices(User user) {
        List<TvDevice> devices = tvDeviceRepository.findByUserAndActiveTrue(user);
        return devices.stream()
                .map(this::mapToTvDeviceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate TV device
     */
    @Transactional
    public void deactivateDevice(User user, Long deviceId) {
        TvDevice device = tvDeviceRepository.findActiveDeviceByUserAndId(user, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found or not owned by user"));

        device.setActive(false);
        tvDeviceRepository.save(device);

        logger.info("TV device deactivated: {} for user: {}", device.getDeviceName(), user.getEmail());
    }

    /**
     * Update device last seen timestamp (called by WebOS app)
     */
    @Transactional
    public void updateDeviceLastSeen(String macAddress) {
        Optional<TvDevice> deviceOpt = tvDeviceRepository.findByMacAddress(macAddress);
        if (deviceOpt.isPresent()) {
            TvDevice device = deviceOpt.get();
            device.updateLastSeen();
            tvDeviceRepository.save(device);
        }
    }

    /**
     * Authenticate device by MAC address (for WebOS app API calls)
     */
    public Optional<TvDevice> authenticateDevice(String macAddress) {
        return tvDeviceRepository.findByMacAddress(macAddress)
                .filter(TvDevice::isActive);
    }

    /**
     * Clean up expired pairing tokens
     */
    @Transactional
    public int cleanupExpiredTokens() {
        List<DevicePairingToken> expiredTokens = pairingTokenRepository.findExpiredTokens(LocalDateTime.now());
        
        for (DevicePairingToken token : expiredTokens) {
            if (!token.isUsed()) {
                token.setUsed(true); // Mark as used to clean up
                pairingTokenRepository.save(token);
            }
        }

        logger.info("Cleaned up {} expired pairing tokens", expiredTokens.size());
        return expiredTokens.size();
    }

    private String generatePairingToken() {
        StringBuilder token = new StringBuilder(PAIRING_TOKEN_LENGTH);
        for (int i = 0; i < PAIRING_TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(secureRandom.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }

    private String hashMacAddress(String macAddress) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(macAddress.getBytes());
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

    private String maskMacAddress(String macAddress) {
        if (macAddress == null || macAddress.length() < 8) {
            return "**:**:**:**:**:**";
        }
        // Show first 8 characters, mask the rest
        return macAddress.substring(0, 8) + "**:**:**";
    }

    private TvDeviceResponse mapToTvDeviceResponse(TvDevice device) {
        TvDeviceResponse response = new TvDeviceResponse();
        response.setId(device.getId());
        response.setDeviceName(device.getDeviceName());
        response.setMacAddress(maskMacAddress(device.getMacAddress())); // Masked for security
        response.setWebosVersion(device.getWebosVersion());
        response.setModelName(device.getModelName());
        response.setManufacturer(device.getManufacturer());
        response.setScreenResolution(device.getScreenResolution());
        response.setActive(device.isActive());
        response.setRecentlyActive(device.isRecentlyActive());
        response.setLastSeenAt(device.getLastSeenAt());
        response.setPairedAt(device.getPairedAt());
        response.setCreatedAt(device.getCreatedAt());
        response.setUpdatedAt(device.getUpdatedAt());
        return response;
    }
}