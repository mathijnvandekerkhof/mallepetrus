package nl.mallepetrus.jiptv.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
@EntityListeners(AuditingEntityListener.class)
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_fingerprint_id")
    private DeviceFingerprint deviceFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SecurityEventType eventType;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0;

    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "location_country")
    private String locationCountry;

    @Column(name = "location_city")
    private String locationCity;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public SecurityEvent() {}

    public SecurityEvent(User user, SecurityEventType eventType, Integer riskScore) {
        this.user = user;
        this.eventType = eventType;
        this.riskScore = riskScore;
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

    public DeviceFingerprint getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(DeviceFingerprint deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public SecurityEventType getEventType() {
        return eventType;
    }

    public void setEventType(SecurityEventType eventType) {
        this.eventType = eventType;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Enum for event types
    public enum SecurityEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        MFA_SUCCESS,
        MFA_FAILURE,
        NEW_DEVICE_DETECTED,
        SUSPICIOUS_LOCATION,
        RAPID_LOCATION_CHANGE,
        MULTIPLE_FAILED_ATTEMPTS,
        UNUSUAL_TIME_ACCESS,
        CONCURRENT_SESSIONS_EXCEEDED,
        MFA_BYPASS_ATTEMPT,
        DEVICE_TRUSTED,
        DEVICE_UNTRUSTED,
        STEP_UP_AUTH_REQUIRED,
        STEP_UP_AUTH_SUCCESS,
        SESSION_TERMINATED
    }
}