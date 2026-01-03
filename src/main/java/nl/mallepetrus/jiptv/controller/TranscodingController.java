package nl.mallepetrus.jiptv.controller;

import nl.mallepetrus.jiptv.dto.*;
import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.StreamTrack;
import nl.mallepetrus.jiptv.entity.TranscodingJob;
import nl.mallepetrus.jiptv.repository.StreamSourceRepository;
import nl.mallepetrus.jiptv.repository.StreamTrackRepository;
import nl.mallepetrus.jiptv.repository.TranscodingJobRepository;
import nl.mallepetrus.jiptv.service.FFmpegTranscodingService;
import nl.mallepetrus.jiptv.service.TranscodingJobQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transcoding")
public class TranscodingController {

    private static final Logger logger = LoggerFactory.getLogger(TranscodingController.class);

    private final TranscodingJobQueueService queueService;
    private final FFmpegTranscodingService transcodingService;
    private final TranscodingJobRepository jobRepository;
    private final StreamSourceRepository streamSourceRepository;
    private final StreamTrackRepository streamTrackRepository;

    @Autowired
    public TranscodingController(TranscodingJobQueueService queueService,
                               FFmpegTranscodingService transcodingService,
                               TranscodingJobRepository jobRepository,
                               StreamSourceRepository streamSourceRepository,
                               StreamTrackRepository streamTrackRepository) {
        this.queueService = queueService;
        this.transcodingService = transcodingService;
        this.jobRepository = jobRepository;
        this.streamSourceRepository = streamSourceRepository;
        this.streamTrackRepository = streamTrackRepository;
    }

    /**
     * Start transcoding job for WebOS compatibility
     */
    @PostMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TranscodingJobResponse> startTranscodingJob(@Valid @RequestBody TranscodingJobRequest request) {
        logger.info("Starting transcoding job for stream: {}", request.getStreamSourceId());

        // Validate stream source exists
        StreamSource streamSource = streamSourceRepository.findById(request.getStreamSourceId())
                .orElseThrow(() -> new RuntimeException("Stream source not found: " + request.getStreamSourceId()));

        // Validate and get selected tracks
        List<StreamTrack> selectedTracks = streamTrackRepository.findAllById(request.getSelectedTrackIds());
        if (selectedTracks.size() != request.getSelectedTrackIds().size()) {
            throw new RuntimeException("One or more selected tracks not found");
        }

        // Validate tracks belong to the stream
        boolean allTracksValid = selectedTracks.stream()
                .allMatch(track -> track.getStreamSource().getId().equals(request.getStreamSourceId()));
        if (!allTracksValid) {
            throw new RuntimeException("Selected tracks do not belong to the specified stream");
        }

        TranscodingJob job;
        if (request.isGenerateHLS()) {
            // Queue HLS generation job
            job = queueService.queueHLSJob(streamSource, selectedTracks);
        } else {
            // Queue regular transcoding job
            job = queueService.queueTranscodingJob(streamSource, selectedTracks);
        }

        return ResponseEntity.ok(new TranscodingJobResponse(job));
    }

    /**
     * Get transcoding job by ID
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<TranscodingJobResponse> getTranscodingJob(@PathVariable Long jobId) {
        TranscodingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Transcoding job not found: " + jobId));

        return ResponseEntity.ok(new TranscodingJobResponse(job));
    }

    /**
     * Get all transcoding jobs with pagination
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<TranscodingJobResponse>> getTranscodingJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TranscodingJob.Status status,
            @RequestParam(required = false) TranscodingJob.JobType jobType) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TranscodingJob> jobs;

        if (status != null) {
            jobs = jobRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }

        Page<TranscodingJobResponse> response = jobs.map(TranscodingJobResponse::new);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transcoding jobs for specific stream
     */
    @GetMapping("/streams/{streamId}/jobs")
    public ResponseEntity<List<TranscodingJobResponse>> getJobsForStream(@PathVariable Long streamId) {
        List<TranscodingJob> jobs = jobRepository.findByStreamSourceIdOrderByCreatedAtDesc(streamId);
        List<TranscodingJobResponse> response = jobs.stream()
                .map(TranscodingJobResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel transcoding job
     */
    @PostMapping("/jobs/{jobId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TranscodingJobResponse> cancelTranscodingJob(@PathVariable Long jobId) {
        logger.info("Cancelling transcoding job: {}", jobId);

        TranscodingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Transcoding job not found: " + jobId));

        if (job.isFinished()) {
            throw new RuntimeException("Cannot cancel finished job: " + jobId);
        }

        job.cancel();
        job = jobRepository.save(job);

        return ResponseEntity.ok(new TranscodingJobResponse(job));
    }

    /**
     * Cancel all jobs for a stream
     */
    @PostMapping("/streams/{streamId}/jobs/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> cancelJobsForStream(@PathVariable Long streamId) {
        logger.info("Cancelling all jobs for stream: {}", streamId);

        StreamSource streamSource = streamSourceRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream source not found: " + streamId));

        int cancelledCount = queueService.cancelJobsForStream(streamSource);

        return ResponseEntity.ok(new ApiResponse("Cancelled " + cancelledCount + " jobs for stream: " + streamSource.getName()));
    }

    /**
     * Get transcoding queue statistics
     */
    @GetMapping("/queue/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TranscodingQueueStatisticsResponse> getQueueStatistics() {
        TranscodingJobQueueService.JobQueueStatistics stats = queueService.getQueueStatistics();
        return ResponseEntity.ok(new TranscodingQueueStatisticsResponse(stats));
    }

    /**
     * Get job logs
     */
    @GetMapping("/jobs/{jobId}/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getJobLogs(@PathVariable Long jobId) {
        TranscodingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Transcoding job not found: " + jobId));

        String logs = job.getFfmpegLog();
        if (logs == null || logs.isEmpty()) {
            logs = "No logs available for this job.";
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * Check FFmpeg availability
     */
    @GetMapping("/ffmpeg/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> checkFFmpegStatus() {
        boolean available = transcodingService.isFFmpegAvailable();
        String message = available ? "FFmpeg is available and ready" : "FFmpeg is not available or not working";
        
        return ResponseEntity.ok(new ApiResponse(message, available));
    }

    /**
     * Get available transcoding profiles
     */
    @GetMapping("/profiles")
    public ResponseEntity<List<TranscodingProfileResponse>> getTranscodingProfiles() {
        List<TranscodingProfileResponse> profiles = List.of(
            new TranscodingProfileResponse("WebOS_Compatible", "WebOS TV Compatible", 
                "Optimized for WebOS TV compatibility with minimal transcoding"),
            new TranscodingProfileResponse("WebOS_Optimized", "WebOS TV Optimized", 
                "Smaller file size optimized for WebOS TV with quality balance")
        );

        return ResponseEntity.ok(profiles);
    }

    /**
     * Get HLS profiles
     */
    @GetMapping("/hls-profiles")
    public ResponseEntity<List<HLSProfileResponse>> getHLSProfiles() {
        List<HLSProfileResponse> profiles = List.of(
            new HLSProfileResponse("WebOS_HLS", "WebOS TV HLS", 
                "HLS streaming optimized for WebOS TV", 6)
        );

        return ResponseEntity.ok(profiles);
    }

    // Response DTOs
    public static class TranscodingProfileResponse {
        private String name;
        private String displayName;
        private String description;

        public TranscodingProfileResponse(String name, String displayName, String description) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static class HLSProfileResponse {
        private String name;
        private String displayName;
        private String description;
        private int segmentDuration;

        public HLSProfileResponse(String name, String displayName, String description, int segmentDuration) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.segmentDuration = segmentDuration;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getSegmentDuration() { return segmentDuration; }
    }
}