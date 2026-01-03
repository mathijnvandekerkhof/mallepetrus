package nl.mallepetrus.jiptv.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tv_devices")
@EntityListeners(AuditingEntityListener.class)
public class TvDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @NotBlank
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", 
             message = "MAC address must be in format XX:XX:XX:XX:XX:XX")
    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @NotBlank
    @Column(name = "mac_address_hash", nullable = false, unique = true)
    private String macAddressHash;

    @Column(name = "webos_version")
    private String webosVersion;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "manufacturer")
    private String manufacturer = "LG";

    @Column(name = "screen_resolution")
    private String screenResolution;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "paired_at", nullable = false)
    private LocalDateTime pairedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public TvDevice() {}

    public TvDevice(User user, String deviceName, String macAddress, String macAddressHash) {
        this.user = user;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.macAddressHash = macAddressHash;
        this.pairedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getMacAddressHash() {
        return macAddressHash;
    }

    public void setMacAddressHash(String macAddressHash) {
        this.macAddressHash = macAddressHash;
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

    // Helper methods
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }

    public boolean isRecentlyActive() {
        if (lastSeenAt == null) return false;
        return lastSeenAt.isAfter(LocalDateTime.now().minusHours(24));
    }
}