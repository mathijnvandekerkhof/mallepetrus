package nl.mallepetrus.jiptv.controller;

import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.DevicePairingRequest;
import nl.mallepetrus.jiptv.dto.QrCodeResponse;
import nl.mallepetrus.jiptv.dto.TvDeviceResponse;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.repository.UserRepository;
import nl.mallepetrus.jiptv.service.DevicePairingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device-pairing")
public class DevicePairingController {

    @Autowired
    private DevicePairingService devicePairingService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Generate QR code for device pairing (User authenticated)
     */
    @PostMapping("/generate-qr")
    public ResponseEntity<?> generatePairingQrCode(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            QrCodeResponse qrCode = devicePairingService.generatePairingQrCode(user);
            return ResponseEntity.ok(qrCode);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate QR code: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Pair device using pairing token (Public endpoint for WebOS TV app)
     */
    @PostMapping("/pair")
    public ResponseEntity<?> pairDevice(@Valid @RequestBody DevicePairingRequest request) {
        try {
            TvDeviceResponse device = devicePairingService.pairDevice(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device paired successfully");
            response.put("device", device);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to pair device: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get user's TV devices (User authenticated)
     */
    @GetMapping("/devices")
    public ResponseEntity<?> getUserDevices(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<TvDeviceResponse> devices = devicePairingService.getUserDevices(user);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get devices: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get active TV devices (User authenticated)
     */
    @GetMapping("/devices/active")
    public ResponseEntity<?> getActiveDevices(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<TvDeviceResponse> devices = devicePairingService.getActiveUserDevices(user);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get active devices: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Deactivate TV device (User authenticated)
     */
    @PostMapping("/devices/{deviceId}/deactivate")
    public ResponseEntity<?> deactivateDevice(@PathVariable Long deviceId, 
                                             Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            devicePairingService.deactivateDevice(user, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device deactivated successfully");
            response.put("deviceId", deviceId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to deactivate device: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Device heartbeat - update last seen (Public endpoint for WebOS TV app)
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<?> deviceHeartbeat(@RequestParam String macAddress) {
        try {
            devicePairingService.updateDeviceLastSeen(macAddress);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Heartbeat received");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process heartbeat: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Validate pairing token (Public endpoint for WebOS TV app)
     */
    @GetMapping("/validate/{pairingToken}")
    public ResponseEntity<?> validatePairingToken(@PathVariable String pairingToken) {
        try {
            // This endpoint allows WebOS TV app to validate token before showing pairing form
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "Token is valid");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", "Invalid token");
            return ResponseEntity.ok(response); // Return 200 with valid=false
        }
    }

    /**
     * Clean up expired pairing tokens (Admin only)
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupExpiredTokens(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Only admins can trigger cleanup
            if (user.getRole().name().equals("ADMIN")) {
                int cleanedUp = devicePairingService.cleanupExpiredTokens();
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Cleanup completed successfully");
                response.put("cleanedUpCount", cleanedUp);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only administrators can perform cleanup");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to cleanup tokens: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}