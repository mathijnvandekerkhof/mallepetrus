package nl.mallepetrus.jiptv.dto;

import java.time.LocalDateTime;

public class QrCodeResponse {
    
    private String pairingToken;
    private String qrCodeData;
    private String qrCodeBase64; // Base64 encoded QR code image
    private LocalDateTime expiresAt;
    private int expiresInMinutes;
    private String pairingUrl; // URL for manual pairing if QR scan fails
    
    // Constructors
    public QrCodeResponse() {}
    
    public QrCodeResponse(String pairingToken, String qrCodeData, String qrCodeBase64, 
                         LocalDateTime expiresAt, String pairingUrl) {
        this.pairingToken = pairingToken;
        this.qrCodeData = qrCodeData;
        this.qrCodeBase64 = qrCodeBase64;
        this.expiresAt = expiresAt;
        this.pairingUrl = pairingUrl;
        
        // Calculate minutes until expiry
        if (expiresAt != null) {
            long minutesUntilExpiry = java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
            this.expiresInMinutes = (int) Math.max(0, minutesUntilExpiry);
        }
    }
    
    // Getters and Setters
    public String getPairingToken() {
        return pairingToken;
    }
    
    public void setPairingToken(String pairingToken) {
        this.pairingToken = pairingToken;
    }
    
    public String getQrCodeData() {
        return qrCodeData;
    }
    
    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }
    
    public String getQrCodeBase64() {
        return qrCodeBase64;
    }
    
    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        
        // Recalculate minutes until expiry
        if (expiresAt != null) {
            long minutesUntilExpiry = java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
            this.expiresInMinutes = (int) Math.max(0, minutesUntilExpiry);
        }
    }
    
    public int getExpiresInMinutes() {
        return expiresInMinutes;
    }
    
    public void setExpiresInMinutes(int expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
    
    public String getPairingUrl() {
        return pairingUrl;
    }
    
    public void setPairingUrl(String pairingUrl) {
        this.pairingUrl = pairingUrl;
    }
}