package nl.mallepetrus.jiptv.dto;

import nl.mallepetrus.jiptv.entity.TranscodingJob;

import java.time.LocalDateTime;

public class TranscodingJobResponse {

    private Long id;
    private Long streamSourceId;
    private String streamSourceName;
    private TranscodingJob.JobType jobType;
    private TranscodingJob.Status status;
    private String transcodingProfile;
    private Integer progressPercent;
    private Long currentFrame;
    private Long totalFrames;
    private String processingSpeed;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime estimatedCompletionAt;
    private String outputFile;
    private String formattedOutputSize;
    private String errorMessage;
    private long durationMinutes;
    private LocalDateTime createdAt;

    // Constructors
    public TranscodingJobResponse() {}

    public TranscodingJobResponse(TranscodingJob job) {
        this.id = job.getId();
        this.streamSourceId = job.getStreamSource().getId();
        this.streamSourceName = job.getStreamSource().getName();
        this.jobType = job.getJobType();
        this.status = job.getStatus();
        this.transcodingProfile = job.getTranscodingProfile();
        this.progressPercent = job.getProgressPercent();
        this.currentFrame = job.getCurrentFrame();
        this.totalFrames = job.getTotalFrames();
        this.processingSpeed = job.getProcessingSpeed();
        this.startedAt = job.getStartedAt();
        this.completedAt = job.getCompletedAt();
        this.estimatedCompletionAt = job.getEstimatedCompletionAt();
        this.outputFile = job.getOutputFile();
        this.formattedOutputSize = job.getFormattedOutputSize();
        this.errorMessage = job.getErrorMessage();
        this.durationMinutes = job.getDurationMinutes();
        this.createdAt = job.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStreamSourceId() {
        return streamSourceId;
    }

    public void setStreamSourceId(Long streamSourceId) {
        this.streamSourceId = streamSourceId;
    }

    public String getStreamSourceName() {
        return streamSourceName;
    }

    public void setStreamSourceName(String streamSourceName) {
        this.streamSourceName = streamSourceName;
    }

    public TranscodingJob.JobType getJobType() {
        return jobType;
    }

    public void setJobType(TranscodingJob.JobType jobType) {
        this.jobType = jobType;
    }

    public TranscodingJob.Status getStatus() {
        return status;
    }

    public void setStatus(TranscodingJob.Status status) {
        this.status = status;
    }

    public String getTranscodingProfile() {
        return transcodingProfile;
    }

    public void setTranscodingProfile(String transcodingProfile) {
        this.transcodingProfile = transcodingProfile;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Long getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(Long currentFrame) {
        this.currentFrame = currentFrame;
    }

    public Long getTotalFrames() {
        return totalFrames;
    }

    public void setTotalFrames(Long totalFrames) {
        this.totalFrames = totalFrames;
    }

    public String getProcessingSpeed() {
        return processingSpeed;
    }

    public void setProcessingSpeed(String processingSpeed) {
        this.processingSpeed = processingSpeed;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getEstimatedCompletionAt() {
        return estimatedCompletionAt;
    }

    public void setEstimatedCompletionAt(LocalDateTime estimatedCompletionAt) {
        this.estimatedCompletionAt = estimatedCompletionAt;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getFormattedOutputSize() {
        return formattedOutputSize;
    }

    public void setFormattedOutputSize(String formattedOutputSize) {
        this.formattedOutputSize = formattedOutputSize;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}