package nl.mallepetrus.jiptv.dto;

public class MfaSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private String manualEntryKey;
    private boolean enabled;

    // Constructors
    public MfaSetupResponse() {}

    public MfaSetupResponse(String secret, String qrCodeUrl, String manualEntryKey, boolean enabled) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
        this.manualEntryKey = manualEntryKey;
        this.enabled = enabled;
    }

    // Getters and Setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getManualEntryKey() {
        return manualEntryKey;
    }

    public void setManualEntryKey(String manualEntryKey) {
        this.manualEntryKey = manualEntryKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}