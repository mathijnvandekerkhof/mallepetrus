package nl.mallepetrus.jiptv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaVerificationRequest {
    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "\\d{6}", message = "TOTP code must be 6 digits")
    private String totpCode;

    // Constructors
    public MfaVerificationRequest() {}

    public MfaVerificationRequest(String totpCode) {
        this.totpCode = totpCode;
    }

    // Getters and Setters
    public String getTotpCode() {
        return totpCode;
    }

    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
}