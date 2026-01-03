package nl.mallepetrus.jiptv.dto;

public class UpdateStreamPreferencesRequest {
    
    private Long videoTrackId;
    private Long audioTrackId;
    private Long subtitleTrackId;
    private boolean subtitleEnabled = false;
    private String quality = "AUTO"; // AUTO, ORIGINAL, 1080p, 720p, 480p, 360p

    public UpdateStreamPreferencesRequest() {}

    public UpdateStreamPreferencesRequest(Long videoTrackId, Long audioTrackId, 
                                        Long subtitleTrackId, boolean subtitleEnabled, String quality) {
        this.videoTrackId = videoTrackId;
        this.audioTrackId = audioTrackId;
        this.subtitleTrackId = subtitleTrackId;
        this.subtitleEnabled = subtitleEnabled;
        this.quality = quality;
    }

    // Getters and Setters
    public Long getVideoTrackId() {
        return videoTrackId;
    }

    public void setVideoTrackId(Long videoTrackId) {
        this.videoTrackId = videoTrackId;
    }

    public Long getAudioTrackId() {
        return audioTrackId;
    }

    public void setAudioTrackId(Long audioTrackId) {
        this.audioTrackId = audioTrackId;
    }

    public Long getSubtitleTrackId() {
        return subtitleTrackId;
    }

    public void setSubtitleTrackId(Long subtitleTrackId) {
        this.subtitleTrackId = subtitleTrackId;
    }

    public boolean isSubtitleEnabled() {
        return subtitleEnabled;
    }

    public void setSubtitleEnabled(boolean subtitleEnabled) {
        this.subtitleEnabled = subtitleEnabled;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }
}