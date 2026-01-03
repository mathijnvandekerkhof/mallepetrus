package nl.mallepetrus.jiptv.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stream_sessions")
@EntityListeners(AuditingEntityListener.class)
public class StreamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_device_id", nullable = false)
    private TvDevice tvDevice;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_source_id", nullable = false)
    private StreamSource streamSource;

    @NotBlank
    @Column(name = "session_token", nullable = false, unique = true, length = 128)
    private String sessionToken;

    // Selected tracks for this session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_video_track_id")
    private StreamTrack selectedVideoTrack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_audio_track_id")
    private StreamTrack selectedAudioTrack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_subtitle_track_id")
    private StreamTrack selectedSubtitleTrack;

    // Streaming details
    @Column(name = "stream_url", columnDefinition = "TEXT")
    private String streamUrl;

    @Enumerated(EnumType.STRING)
    private Quality quality = Quality.AUTO;

    @Column(name = "transcoding_profile", length = 50)
    private String transcodingProfile;

    // Session state
    @Column(name = "playback_position_seconds", precision = 10, scale = 3)
    private BigDecimal playbackPositionSeconds = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public StreamSession() {
        this.startedAt = LocalDateTime.now();
        this.lastHeartbeatAt = LocalDateTime.now();
    }

    public StreamSession(User user, TvDevice tvDevice, StreamSource streamSource, String sessionToken) {
        this();
        this.user = user;
        this.tvDevice = tvDevice;
        this.streamSource = streamSource;
        this.sessionToken = sessionToken;
    }

    // Enums
    public enum Quality {
        AUTO,
        ORIGINAL,
        HD_1080P("1080p"),
        HD_720P("720p"),
        SD_480P("480p"),
        SD_360P("360p");

        private final String displayName;

        Quality() {
            this.displayName = name();
        }

        Quality(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
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

    public TvDevice getTvDevice() {
        return tvDevice;
    }

    public void setTvDevice(TvDevice tvDevice) {
        this.tvDevice = tvDevice;
    }

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public StreamTrack getSelectedVideoTrack() {
        return selectedVideoTrack;
    }

    public void setSelectedVideoTrack(StreamTrack selectedVideoTrack) {
        this.selectedVideoTrack = selectedVideoTrack;
    }

    public StreamTrack getSelectedAudioTrack() {
        return selectedAudioTrack;
    }

    public void setSelectedAudioTrack(StreamTrack selectedAudioTrack) {
        this.selectedAudioTrack = selectedAudioTrack;
    }

    public StreamTrack getSelectedSubtitleTrack() {
        return selectedSubtitleTrack;
    }

    public void setSelectedSubtitleTrack(StreamTrack selectedSubtitleTrack) {
        this.selectedSubtitleTrack = selectedSubtitleTrack;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public String getTranscodingProfile() {
        return transcodingProfile;
    }

    public void setTranscodingProfile(String transcodingProfile) {
        this.transcodingProfile = transcodingProfile;
    }

    public BigDecimal getPlaybackPositionSeconds() {
        return playbackPositionSeconds;
    }

    public void setPlaybackPositionSeconds(BigDecimal playbackPositionSeconds) {
        this.playbackPositionSeconds = playbackPositionSeconds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
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
    public void updateHeartbeat() {
        this.lastHeartbeatAt = LocalDateTime.now();
    }

    public void endSession() {
        this.active = false;
        this.endedAt = LocalDateTime.now();
    }

    public boolean isExpired(int timeoutMinutes) {
        return lastHeartbeatAt.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    public long getDurationMinutes() {
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).toMinutes();
    }

    public String getFormattedPlaybackPosition() {
        if (playbackPositionSeconds == null) return "00:00";
        
        int totalSeconds = playbackPositionSeconds.intValue();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public boolean hasSelectedTracks() {
        return selectedVideoTrack != null || selectedAudioTrack != null || selectedSubtitleTrack != null;
    }
}