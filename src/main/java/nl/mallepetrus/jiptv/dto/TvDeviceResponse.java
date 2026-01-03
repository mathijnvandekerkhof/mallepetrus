package nl.mallepetrus.jiptv.dto;

import java.time.LocalDateTime;

public class TvDeviceResponse {
    
    private Long id;
    private String deviceName;
    private String macAddress; // Masked for security (XX:XX:XX:XX:XX:XX -> XX:XX:XX:**:**:**)
    private String webosVersion;
    private String modelName;
    private String manufacturer;
    private String screenResolution;
    private boolean active;
    private boolean recentlyActive;
    private LocalDateTime lastSeenAt;
    private LocalDateTime pairedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public TvDeviceResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public String getWebosVersion() {
        return webosVersion;
    }
    
    public void setWebosVersion(String webosVersion) {
        this.webosVersion = webosVersion;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getScreenResolution() {
        return screenResolution;
    }
    
    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isRecentlyActive() {
        return recentlyActive;
    }
    
    public void setRecentlyActive(boolean recentlyActive) {
        this.recentlyActive = recentlyActive;
    }
    
    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }
    
    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
    
    public LocalDateTime getPairedAt() {
        return pairedAt;
    }
    
    public void setPairedAt(LocalDateTime pairedAt) {
        this.pairedAt = pairedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}