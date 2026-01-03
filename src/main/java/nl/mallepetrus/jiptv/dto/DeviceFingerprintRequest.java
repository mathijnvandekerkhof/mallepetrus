package nl.mallepetrus.jiptv.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceFingerprintRequest {
    @NotBlank(message = "User agent is required")
    private String userAgent;

    private String screenResolution;
    private String timezone;
    private String language;
    private String platform;
    private String deviceName;

    // Constructors
    public DeviceFingerprintRequest() {}

    // Getters and Setters
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}