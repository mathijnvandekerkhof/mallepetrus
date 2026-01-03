package nl.mallepetrus.jiptv.controller;

import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.MfaSetupRequest;
import nl.mallepetrus.jiptv.dto.MfaSetupResponse;
import nl.mallepetrus.jiptv.dto.MfaVerificationRequest;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.repository.UserRepository;
import nl.mallepetrus.jiptv.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mfa")
public class MfaController {

    @Autowired
    private MfaService mfaService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/status")
    public ResponseEntity<?> getMfaStatus(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            MfaSetupResponse status = mfaService.getMfaStatus(user);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get MFA status: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setupMfa(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (user.isMfaEnabled()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "MFA is already enabled for this user");
                return ResponseEntity.badRequest().body(error);
            }

            MfaSetupResponse setupResponse = mfaService.generateMfaSetup(user);
            return ResponseEntity.ok(setupResponse);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to setup MFA: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enableMfa(@Valid @RequestBody MfaSetupRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (user.isMfaEnabled()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "MFA is already enabled for this user");
                return ResponseEntity.badRequest().body(error);
            }

            boolean success = mfaService.enableMfa(user, request.getTotpCode());
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "MFA enabled successfully");
                response.put("enabled", true);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid TOTP code");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to enable MFA: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(@Valid @RequestBody MfaVerificationRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.isMfaEnabled()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "MFA is not enabled for this user");
                return ResponseEntity.badRequest().body(error);
            }

            boolean success = mfaService.disableMfa(user, request.getTotpCode());
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "MFA disabled successfully");
                response.put("enabled", false);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid TOTP code");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to disable MFA: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(@Valid @RequestBody MfaVerificationRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.isMfaEnabled()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "MFA is not enabled for this user");
                return ResponseEntity.badRequest().body(error);
            }

            boolean valid = mfaService.verifyTotpCode(user.getMfaSecret(), request.getTotpCode());
            Map<String, Object> response = new HashMap<>();
            response.put("valid", valid);
            response.put("message", valid ? "TOTP code is valid" : "Invalid TOTP code");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to verify MFA: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}