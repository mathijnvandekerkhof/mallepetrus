package nl.mallepetrus.jiptv.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_stream_preferences")
@EntityListeners(AuditingEntityListener.class)
public class UserStreamPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_source_id", nullable = false)
    private StreamSource streamSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_video_track_id")
    private StreamTrack preferredVideoTrack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_audio_track_id")
    private StreamTrack preferredAudioTrack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_subtitle_track_id")
    private StreamTrack preferredSubtitleTrack;

    @Column(name = "subtitle_enabled", nullable = false)
    private boolean subtitleEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_quality")
    private Quality preferredQuality = Quality.AUTO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserStreamPreferences() {}

    public UserStreamPreferences(User user, StreamSource streamSource) {
        this.user = user;
        this.streamSource = streamSource;
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

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

    public StreamTrack getPreferredVideoTrack() {
        return preferredVideoTrack;
    }

    public void setPreferredVideoTrack(StreamTrack preferredVideoTrack) {
        this.preferredVideoTrack = preferredVideoTrack;
    }

    public StreamTrack getPreferredAudioTrack() {
        return preferredAudioTrack;
    }

    public void setPreferredAudioTrack(StreamTrack preferredAudioTrack) {
        this.preferredAudioTrack = preferredAudioTrack;
    }

    public StreamTrack getPreferredSubtitleTrack() {
        return preferredSubtitleTrack;
    }

    public void setPreferredSubtitleTrack(StreamTrack preferredSubtitleTrack) {
        this.preferredSubtitleTrack = preferredSubtitleTrack;
    }

    public boolean isSubtitleEnabled() {
        return subtitleEnabled;
    }

    public void setSubtitleEnabled(boolean subtitleEnabled) {
        this.subtitleEnabled = subtitleEnabled;
    }

    public Quality getPreferredQuality() {
        return preferredQuality;
    }

    public void setPreferredQuality(Quality preferredQuality) {
        this.preferredQuality = preferredQuality;
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
    public boolean hasVideoPreference() {
        return preferredVideoTrack != null;
    }

    public boolean hasAudioPreference() {
        return preferredAudioTrack != null;
    }

    public boolean hasSubtitlePreference() {
        return preferredSubtitleTrack != null && subtitleEnabled;
    }

    public boolean hasAnyPreferences() {
        return hasVideoPreference() || hasAudioPreference() || hasSubtitlePreference();
    }
}