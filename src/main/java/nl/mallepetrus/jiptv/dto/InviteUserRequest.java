package nl.mallepetrus.jiptv.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InviteUserRequest {
    
    @Email(message = "Valid email address is required")
    @NotBlank(message = "Email is required")
    private String email;
    
    private String message; // Optional personal message in invitation email
    
    // Constructors
    public InviteUserRequest() {}
    
    public InviteUserRequest(String email, String message) {
        this.email = email;
        this.message = message;
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}