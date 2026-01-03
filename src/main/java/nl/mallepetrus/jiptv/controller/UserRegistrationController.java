package nl.mallepetrus.jiptv.controller;

import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.UserRegistrationRequest;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/register")
public class UserRegistrationController {

    @Autowired
    private InvitationService invitationService;

    /**
     * Register new user with invitation code
     */
    @PostMapping
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            User newUser = invitationService.registerUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful");
            response.put("userId", newUser.getId());
            response.put("email", newUser.getEmail());
            response.put("emailVerified", newUser.isEmailVerified());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Validate invitation code (for frontend validation)
     */
    @GetMapping("/validate/{invitationCode}")
    public ResponseEntity<?> validateInvitationCode(@PathVariable String invitationCode) {
        try {
            var invitation = invitationService.getInvitationByCode(invitationCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", invitation.isValid());
            response.put("email", invitation.getEmail());
            response.put("expired", invitation.isExpired());
            response.put("used", invitation.isUsed());
            response.put("expiresAt", invitation.getExpiresAt());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response); // Return 200 with valid=false instead of 400
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to validate invitation code");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}