package nl.mallepetrus.jiptv.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcoding_jobs")
@EntityListeners(AuditingEntityListener.class)
public class TranscodingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_source_id", nullable = false)
    private StreamSource streamSource;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    // Job parameters
    @NotBlank
    @Column(name = "input_file", nullable = false, columnDefinition = "TEXT")
    private String inputFile;

    @Column(name = "output_file", columnDefinition = "TEXT")
    private String outputFile;

    @Column(name = "ffmpeg_command", columnDefinition = "TEXT")
    private String ffmpegCommand;

    @Column(name = "transcoding_profile", length = 50)
    private String transcodingProfile;

    @Column(name = "target_tracks", columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String targetTracks; // JSON string with selected tracks

    // Progress tracking
    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @Column(name = "current_frame")
    private Long currentFrame = 0L;

    @Column(name = "total_frames")
    private Long totalFrames;

    @Column(name = "processing_speed", length = 20)
    private String processingSpeed; // "2.5x", "1.2x"

    // Timing
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "estimated_completion_at")
    private LocalDateTime estimatedCompletionAt;

    // Results
    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ffmpeg_log", columnDefinition = "TEXT")
    private String ffmpegLog;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public TranscodingJob() {}

    public TranscodingJob(StreamSource streamSource, JobType jobType, String inputFile) {
        this.streamSource = streamSource;
        this.jobType = jobType;
        this.inputFile = inputFile;
    }

    // Enums
    public enum JobType {
        ANALYSIS,
        TRANSCODE,
        THUMBNAIL,
        SEGMENT
    }

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getFfmpegCommand() {
        return ffmpegCommand;
    }

    public void setFfmpegCommand(String ffmpegCommand) {
        this.ffmpegCommand = ffmpegCommand;
    }

    public String getTranscodingProfile() {
        return transcodingProfile;
    }

    public void setTranscodingProfile(String transcodingProfile) {
        this.transcodingProfile = transcodingProfile;
    }

    public String getTargetTracks() {
        return targetTracks;
    }

    public void setTargetTracks(String targetTracks) {
        this.targetTracks = targetTracks;
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

    public Long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    public void setOutputSizeBytes(Long outputSizeBytes) {
        this.outputSizeBytes = outputSizeBytes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFfmpegLog() {
        return ffmpegLog;
    }

    public void setFfmpegLog(String ffmpegLog) {
        this.ffmpegLog = ffmpegLog;
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
    public void start() {
        this.status = Status.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercent = 100;
    }

    public void fail(String errorMessage) {
        this.status = Status.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isFinished() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }

    public long getDurationMinutes() {
        if (startedAt == null) return 0;
        
        LocalDateTime end = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).toMinutes();
    }

    public String getFormattedOutputSize() {
        if (outputSizeBytes == null) return "Unknown";
        
        double size = outputSizeBytes.doubleValue();
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
}