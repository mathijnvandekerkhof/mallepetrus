package nl.mallepetrus.jiptv.dto;

import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.StreamTrack;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class StreamSourceResponse {
    private Long id;
    private String name;
    private String description;
    private String sourceType;
    private String sourceUrl;
    private Long fileSize;
    private String formattedFileSize;
    private String contentType;
    private Integer durationSeconds;
    private String formattedDuration;
    private boolean active;
    private boolean analyzed;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
    private List<StreamTrackResponse> tracks;
    private TrackSummary trackSummary;

    public StreamSourceResponse() {}

    public StreamSourceResponse(StreamSource streamSource) {
        this.id = streamSource.getId();
        this.name = streamSource.getName();
        this.description = streamSource.getDescription();
        this.sourceType = streamSource.getSourceType().name();
        this.sourceUrl = streamSource.getSourceUrl();
        this.fileSize = streamSource.getFileSize();
        this.formattedFileSize = streamSource.getFormattedFileSize();
        this.contentType = streamSource.getContentType();
        this.durationSeconds = streamSource.getDurationSeconds();
        this.formattedDuration = streamSource.getFormattedDuration();
        this.active = streamSource.isActive();
        this.analyzed = streamSource.isAnalyzed();
        this.analyzedAt = streamSource.getAnalyzedAt();
        this.createdAt = streamSource.getCreatedAt();
        
        if (streamSource.getTracks() != null) {
            this.tracks = streamSource.getTracks().stream()
                    .map(StreamTrackResponse::new)
                    .collect(Collectors.toList());
            this.trackSummary = new TrackSummary(streamSource.getTracks());
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFormattedFileSize() { return formattedFileSize; }
    public void setFormattedFileSize(String formattedFileSize) { this.formattedFileSize = formattedFileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getFormattedDuration() { return formattedDuration; }
    public void setFormattedDuration(String formattedDuration) { this.formattedDuration = formattedDuration; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isAnalyzed() { return analyzed; }
    public void setAnalyzed(boolean analyzed) { this.analyzed = analyzed; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<StreamTrackResponse> getTracks() { return tracks; }
    public void setTracks(List<StreamTrackResponse> tracks) { this.tracks = tracks; }

    public TrackSummary getTrackSummary() { return trackSummary; }
    public void setTrackSummary(TrackSummary trackSummary) { this.trackSummary = trackSummary; }

    public static class TrackSummary {
        private int videoTracks;
        private int audioTracks;
        private int subtitleTracks;
        private int webosCompatibleTracks;
        private int transcodingRequiredTracks;
        private List<String> languages;
        private List<String> codecs;

        public TrackSummary() {}

        public TrackSummary(List<StreamTrack> tracks) {
            this.videoTracks = (int) tracks.stream().filter(t -> t.getTrackType() == StreamTrack.TrackType.VIDEO).count();
            this.audioTracks = (int) tracks.stream().filter(t -> t.getTrackType() == StreamTrack.TrackType.AUDIO).count();
            this.subtitleTracks = (int) tracks.stream().filter(t -> t.getTrackType() == StreamTrack.TrackType.SUBTITLE).count();
            this.webosCompatibleTracks = (int) tracks.stream().filter(StreamTrack::isWebosCompatible).count();
            this.transcodingRequiredTracks = (int) tracks.stream().filter(StreamTrack::isTranscodingRequired).count();
            
            this.languages = tracks.stream()
                    .map(StreamTrack::getLanguage)
                    .filter(lang -> lang != null && !lang.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                    
            this.codecs = tracks.stream()
                    .map(StreamTrack::getCodecName)
                    .filter(codec -> codec != null && !codec.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Getters and Setters
        public int getVideoTracks() { return videoTracks; }
        public void setVideoTracks(int videoTracks) { this.videoTracks = videoTracks; }

        public int getAudioTracks() { return audioTracks; }
        public void setAudioTracks(int audioTracks) { this.audioTracks = audioTracks; }

        public int getSubtitleTracks() { return subtitleTracks; }
        public void setSubtitleTracks(int subtitleTracks) { this.subtitleTracks = subtitleTracks; }

        public int getWebosCompatibleTracks() { return webosCompatibleTracks; }
        public void setWebosCompatibleTracks(int webosCompatibleTracks) { this.webosCompatibleTracks = webosCompatibleTracks; }

        public int getTranscodingRequiredTracks() { return transcodingRequiredTracks; }
        public void setTranscodingRequiredTracks(int transcodingRequiredTracks) { this.transcodingRequiredTracks = transcodingRequiredTracks; }

        public List<String> getLanguages() { return languages; }
        public void setLanguages(List<String> languages) { this.languages = languages; }

        public List<String> getCodecs() { return codecs; }
        public void setCodecs(List<String> codecs) { this.codecs = codecs; }
    }
}