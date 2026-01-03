package nl.mallepetrus.jiptv.controller;

import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.SetupRequest;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.setup.SetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/setup")
public class SetupController {

    private static final Logger logger = LoggerFactory.getLogger(SetupController.class);

    @Autowired
    private SetupService setupService;

    /**
     * Get setup status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSetupStatus() {
        try {
            SetupService.SetupStatus status = setupService.getSetupStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting setup status", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get setup status"));
        }
    }

    /**
     * Initialize the application with admin user
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializeSetup(@Valid @RequestBody SetupRequest request) {
        try {
            // Check if setup is needed
            if (!setupService.needsSetup()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Setup has already been completed"));
            }

            // Validate password confirmation
            if (!request.isPasswordMatch()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passwords do not match"));
            }

            // Create admin user
            User admin = setupService.createInitialAdmin(request.getEmail(), request.getPassword());
            
            logger.info("Setup completed successfully for admin: {}", admin.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "message", "Setup completed successfully",
                "adminEmail", admin.getEmail(),
                "adminId", admin.getId()
            ));

        } catch (IllegalStateException e) {
            logger.warn("Setup attempt when already completed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid setup request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during setup initialization", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Setup failed due to internal error"));
        }
    }
}