package nl.mallepetrus.jiptv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DevicePairingRequest {
    
    @NotBlank(message = "Pairing token is required")
    private String pairingToken;
    
    @NotBlank(message = "Device name is required")
    private String deviceName;
    
    @NotBlank(message = "MAC address is required")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", 
             message = "MAC address must be in format XX:XX:XX:XX:XX:XX")
    private String macAddress;
    
    private String webosVersion;
    private String modelName;
    private String manufacturer = "LG";
    private String screenResolution;
    
    // Constructors
    public DevicePairingRequest() {}
    
    // Getters and Setters
    public String getPairingToken() {
        return pairingToken;
    }
    
    public void setPairingToken(String pairingToken) {
        this.pairingToken = pairingToken;
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
}