package nl.mallepetrus.jiptv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TranscodingJobRequest {

    @NotNull(message = "Stream source ID is required")
    private Long streamSourceId;

    @Size(min = 1, message = "At least one track must be selected")
    private List<Long> selectedTrackIds;

    private String transcodingProfile = "WebOS_Compatible";

    private boolean generateHLS = false;

    // Constructors
    public TranscodingJobRequest() {}

    public TranscodingJobRequest(Long streamSourceId, List<Long> selectedTrackIds) {
        this.streamSourceId = streamSourceId;
        this.selectedTrackIds = selectedTrackIds;
    }

    // Getters and Setters
    public Long getStreamSourceId() {
        return streamSourceId;
    }

    public void setStreamSourceId(Long streamSourceId) {
        this.streamSourceId = streamSourceId;
    }

    public List<Long> getSelectedTrackIds() {
        return selectedTrackIds;
    }

    public void setSelectedTrackIds(List<Long> selectedTrackIds) {
        this.selectedTrackIds = selectedTrackIds;
    }

    public String getTranscodingProfile() {
        return transcodingProfile;
    }

    public void setTranscodingProfile(String transcodingProfile) {
        this.transcodingProfile = transcodingProfile;
    }

    public boolean isGenerateHLS() {
        return generateHLS;
    }

    public void setGenerateHLS(boolean generateHLS) {
        this.generateHLS = generateHLS;
    }
}