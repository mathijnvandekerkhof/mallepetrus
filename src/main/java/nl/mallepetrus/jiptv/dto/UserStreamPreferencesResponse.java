package nl.mallepetrus.jiptv.dto;

import nl.mallepetrus.jiptv.entity.UserStreamPreferences;

import java.time.LocalDateTime;

public class UserStreamPreferencesResponse {
    private Long id;
    private Long streamSourceId;
    private StreamTrackResponse preferredVideoTrack;
    private StreamTrackResponse preferredAudioTrack;
    private StreamTrackResponse preferredSubtitleTrack;
    private boolean subtitleEnabled;
    private String preferredQuality;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserStreamPreferencesResponse() {}

    public UserStreamPreferencesResponse(UserStreamPreferences preferences) {
        this.id = preferences.getId();
        this.streamSourceId = preferences.getStreamSource().getId();
        
        if (preferences.getPreferredVideoTrack() != null) {
            this.preferredVideoTrack = new StreamTrackResponse(preferences.getPreferredVideoTrack());
        }
        
        if (preferences.getPreferredAudioTrack() != null) {
            this.preferredAudioTrack = new StreamTrackResponse(preferences.getPreferredAudioTrack());
        }
        
        if (preferences.getPreferredSubtitleTrack() != null) {
            this.preferredSubtitleTrack = new StreamTrackResponse(preferences.getPreferredSubtitleTrack());
        }
        
        this.subtitleEnabled = preferences.isSubtitleEnabled();
        this.preferredQuality = preferences.getPreferredQuality().name();
        this.createdAt = preferences.getCreatedAt();
        this.updatedAt = preferences.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStreamSourceId() { return streamSourceId; }
    public void setStreamSourceId(Long streamSourceId) { this.streamSourceId = streamSourceId; }

    public StreamTrackResponse getPreferredVideoTrack() { return preferredVideoTrack; }
    public void setPreferredVideoTrack(StreamTrackResponse preferredVideoTrack) { this.preferredVideoTrack = preferredVideoTrack; }

    public StreamTrackResponse getPreferredAudioTrack() { return preferredAudioTrack; }
    public void setPreferredAudioTrack(StreamTrackResponse preferredAudioTrack) { this.preferredAudioTrack = preferredAudioTrack; }

    public StreamTrackResponse getPreferredSubtitleTrack() { return preferredSubtitleTrack; }
    public void setPreferredSubtitleTrack(StreamTrackResponse preferredSubtitleTrack) { this.preferredSubtitleTrack = preferredSubtitleTrack; }

    public boolean isSubtitleEnabled() { return subtitleEnabled; }
    public void setSubtitleEnabled(boolean subtitleEnabled) { this.subtitleEnabled = subtitleEnabled; }

    public String getPreferredQuality() { return preferredQuality; }
    public void setPreferredQuality(String preferredQuality) { this.preferredQuality = preferredQuality; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}