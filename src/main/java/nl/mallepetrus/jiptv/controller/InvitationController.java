package nl.mallepetrus.jiptv.controller;

import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.InvitationResponse;
import nl.mallepetrus.jiptv.dto.InviteUserRequest;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.repository.UserRepository;
import nl.mallepetrus.jiptv.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/invitations")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Send invitation to new user (Admin only)
     */
    @PostMapping("/invite")
    public ResponseEntity<?> inviteUser(@Valid @RequestBody InviteUserRequest request, 
                                       Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            InvitationResponse invitation = invitationService.inviteUser(request, admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invitation sent successfully");
            response.put("invitation", invitation);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to send invitation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get invitation details by code (public endpoint for registration)
     */
    @GetMapping("/code/{invitationCode}")
    public ResponseEntity<?> getInvitationByCode(@PathVariable String invitationCode) {
        try {
            InvitationResponse invitation = invitationService.getInvitationByCode(invitationCode);
            return ResponseEntity.ok(invitation);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get invitation details");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get all invitations (Admin only)
     */
    @GetMapping
    public ResponseEntity<?> getAllInvitations(Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<InvitationResponse> invitations = invitationService.getAllInvitations(admin);
            return ResponseEntity.ok(invitations);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get invitations");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get pending invitations (Admin only)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingInvitations(Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<InvitationResponse> invitations = invitationService.getPendingInvitations(admin);
            return ResponseEntity.ok(invitations);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get pending invitations");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Cancel invitation (Admin only)
     */
    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<?> cancelInvitation(@PathVariable Long invitationId, 
                                             Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            invitationService.cancelInvitation(invitationId, admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invitation cancelled successfully");
            response.put("invitationId", invitationId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to cancel invitation");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Clean up expired invitations (Admin only)
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupExpiredInvitations(Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Only admins can trigger cleanup
            if (admin.getRole().name().equals("ADMIN")) {
                int cleanedUp = invitationService.cleanupExpiredInvitations();
                
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
            error.put("error", "Failed to cleanup invitations");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}