package nl.mallepetrus.jiptv.service;

import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.StreamTrack;
import nl.mallepetrus.jiptv.entity.TranscodingJob;
import nl.mallepetrus.jiptv.repository.TranscodingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class TranscodingJobQueueService {

    private static final Logger logger = LoggerFactory.getLogger(TranscodingJobQueueService.class);

    private final TranscodingJobRepository jobRepository;
    private final FFmpegTranscodingService transcodingService;

    @Value("${jiptv.transcoding.max-concurrent-jobs:2}")
    private int maxConcurrentJobs;

    @Value("${jiptv.transcoding.job-timeout-hours:4}")
    private int jobTimeoutHours;

    @Value("${jiptv.transcoding.cleanup-days:7}")
    private int cleanupDays;

    @Autowired
    public TranscodingJobQueueService(TranscodingJobRepository jobRepository,
                                    FFmpegTranscodingService transcodingService) {
        this.jobRepository = jobRepository;
        this.transcodingService = transcodingService;
    }

    /**
     * Queue transcoding job for WebOS compatibility
     */
    public TranscodingJob queueTranscodingJob(StreamSource streamSource, List<StreamTrack> selectedTracks) {
        logger.info("Queueing transcoding job for stream: {}", streamSource.getName());
        
        // Check if there's already a pending/running job for this stream
        List<TranscodingJob> existingJobs = jobRepository.findByStreamSourceAndStatusInOrderByCreatedAtDesc(
                streamSource, List.of(TranscodingJob.Status.PENDING, TranscodingJob.Status.RUNNING));
        
        if (!existingJobs.isEmpty()) {
            logger.info("Transcoding job already exists for stream: {}, returning existing job", streamSource.getName());
            return existingJobs.get(0);
        }
        
        // Create new transcoding job
        TranscodingJob job = new TranscodingJob(streamSource, TranscodingJob.JobType.TRANSCODE, 
                                              streamSource.getSourceUrl());
        job.setTranscodingProfile(FFmpegTranscodingService.TranscodingProfile.WEBOS_COMPATIBLE.getName());
        job.setTargetTracks(createTargetTracksJson(selectedTracks));
        
        job = jobRepository.save(job);
        
        logger.info("Created transcoding job: {} for stream: {}", job.getId(), streamSource.getName());
        
        // Try to process immediately if capacity allows
        processNextJobIfCapacityAvailable();
        
        return job;
    }

    /**
     * Queue HLS generation job
     */
    public TranscodingJob queueHLSJob(StreamSource streamSource, List<StreamTrack> selectedTracks) {
        logger.info("Queueing HLS generation job for stream: {}", streamSource.getName());
        
        // Check if there's already a pending/running HLS job for this stream
        List<TranscodingJob> existingJobs = jobRepository.findByStreamSourceAndJobTypeAndStatusInOrderByCreatedAtDesc(
                streamSource, TranscodingJob.JobType.SEGMENT, 
                List.of(TranscodingJob.Status.PENDING, TranscodingJob.Status.RUNNING));
        
        if (!existingJobs.isEmpty()) {
            logger.info("HLS job already exists for stream: {}, returning existing job", streamSource.getName());
            return existingJobs.get(0);
        }
        
        // Create new HLS job
        TranscodingJob job = new TranscodingJob(streamSource, TranscodingJob.JobType.SEGMENT, 
                                              streamSource.getSourceUrl());
        job.setTranscodingProfile(FFmpegTranscodingService.HLSProfile.WEBOS_HLS.getName());
        job.setTargetTracks(createTargetTracksJson(selectedTracks));
        
        job = jobRepository.save(job);
        
        logger.info("Created HLS job: {} for stream: {}", job.getId(), streamSource.getName());
        
        // Try to process immediately if capacity allows
        processNextJobIfCapacityAvailable();
        
        return job;
    }

    /**
     * Process next job in queue if capacity is available
     */
    @Async
    public CompletableFuture<Void> processNextJobIfCapacityAvailable() {
        long runningJobs = jobRepository.countRunningJobs();
        
        if (runningJobs >= maxConcurrentJobs) {
            logger.debug("Maximum concurrent jobs ({}) reached, not starting new job", maxConcurrentJobs);
            return CompletableFuture.completedFuture(null);
        }
        
        Optional<TranscodingJob> nextJob = jobRepository.findNextJobToProcess();
        if (nextJob.isEmpty()) {
            logger.debug("No pending jobs in queue");
            return CompletableFuture.completedFuture(null);
        }
        
        TranscodingJob job = nextJob.get();
        logger.info("Starting transcoding job: {} (type: {})", job.getId(), job.getJobType());
        
        try {
            processTranscodingJob(job);
        } catch (Exception e) {
            logger.error("Failed to process transcoding job: {}", job.getId(), e);
            job.fail("Job processing failed: " + e.getMessage());
            jobRepository.save(job);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process a specific transcoding job
     */
    private void processTranscodingJob(TranscodingJob job) {
        switch (job.getJobType()) {
            case TRANSCODE:
                processTranscodeJob(job);
                break;
            case SEGMENT:
                processHLSJob(job);
                break;
            case ANALYSIS:
                // Analysis jobs are handled by FFmpegService
                logger.warn("Analysis job {} should not be processed by transcoding queue", job.getId());
                break;
            default:
                logger.error("Unknown job type: {} for job: {}", job.getJobType(), job.getId());
                job.fail("Unknown job type: " + job.getJobType());
                jobRepository.save(job);
        }
    }

    /**
     * Process transcoding job
     */
    private void processTranscodeJob(TranscodingJob job) {
        try {
            // Parse selected tracks from job
            List<StreamTrack> selectedTracks = parseTargetTracks(job);
            
            // Get transcoding profile
            FFmpegTranscodingService.TranscodingProfile profile = getTranscodingProfile(job.getTranscodingProfile());
            
            // Start transcoding
            transcodingService.startTranscodingJob(job.getStreamSource(), selectedTracks, profile);
            
        } catch (Exception e) {
            logger.error("Failed to start transcoding for job: {}", job.getId(), e);
            job.fail("Failed to start transcoding: " + e.getMessage());
            jobRepository.save(job);
        }
    }

    /**
     * Process HLS generation job
     */
    private void processHLSJob(TranscodingJob job) {
        try {
            // Parse selected tracks from job
            List<StreamTrack> selectedTracks = parseTargetTracks(job);
            
            // Get HLS profile
            FFmpegTranscodingService.HLSProfile profile = getHLSProfile(job.getTranscodingProfile());
            
            // Start HLS generation
            transcodingService.generateHLSSegments(job.getStreamSource(), selectedTracks, profile);
            
        } catch (Exception e) {
            logger.error("Failed to start HLS generation for job: {}", job.getId(), e);
            job.fail("Failed to start HLS generation: " + e.getMessage());
            jobRepository.save(job);
        }
    }

    /**
     * Get transcoding profile by name
     */
    private FFmpegTranscodingService.TranscodingProfile getTranscodingProfile(String profileName) {
        switch (profileName) {
            case "WebOS_Compatible":
                return FFmpegTranscodingService.TranscodingProfile.WEBOS_COMPATIBLE;
            case "WebOS_Optimized":
                return FFmpegTranscodingService.TranscodingProfile.WEBOS_OPTIMIZED;
            default:
                logger.warn("Unknown transcoding profile: {}, using default", profileName);
                return FFmpegTranscodingService.TranscodingProfile.WEBOS_COMPATIBLE;
        }
    }

    /**
     * Get HLS profile by name
     */
    private FFmpegTranscodingService.HLSProfile getHLSProfile(String profileName) {
        switch (profileName) {
            case "WebOS_HLS":
                return FFmpegTranscodingService.HLSProfile.WEBOS_HLS;
            default:
                logger.warn("Unknown HLS profile: {}, using default", profileName);
                return FFmpegTranscodingService.HLSProfile.WEBOS_HLS;
        }
    }

    /**
     * Parse target tracks from JSON
     */
    private List<StreamTrack> parseTargetTracks(TranscodingJob job) {
        // For now, return empty list - in production this would parse the JSON
        // and return the actual StreamTrack objects from the database
        // This is a simplified implementation for the current scope
        
        // TODO: Implement proper JSON parsing and track retrieval
        // The JSON format is: {"video":{"id":1,"index":0,"codec":"h264"},"audio":{"id":2,"index":1,"codec":"aac"}}
        
        return List.of();
    }

    /**
     * Create JSON string for target tracks
     */
    private String createTargetTracksJson(List<StreamTrack> tracks) {
        StringBuilder json = new StringBuilder("{");
        
        for (int i = 0; i < tracks.size(); i++) {
            StreamTrack track = tracks.get(i);
            if (i > 0) json.append(",");
            
            json.append("\"").append(track.getTrackType().name().toLowerCase()).append("\":")
                .append("{\"id\":").append(track.getId())
                .append(",\"index\":").append(track.getTrackIndex())
                .append(",\"codec\":\"").append(track.getCodecName()).append("\"}");
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * Scheduled job to process queue every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void processJobQueue() {
        logger.debug("Processing transcoding job queue...");
        
        long runningJobs = jobRepository.countRunningJobs();
        long pendingJobs = jobRepository.countPendingJobs();
        
        logger.debug("Queue status: {} running, {} pending jobs", runningJobs, pendingJobs);
        
        // Process jobs while we have capacity
        while (runningJobs < maxConcurrentJobs && pendingJobs > 0) {
            processNextJobIfCapacityAvailable();
            runningJobs++;
            pendingJobs--;
        }
    }

    /**
     * Scheduled job to clean up stuck jobs
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupStuckJobs() {
        LocalDateTime stuckThreshold = LocalDateTime.now().minusHours(jobTimeoutHours);
        
        List<TranscodingJob> stuckJobs = jobRepository.findStuckJobs(stuckThreshold);
        
        if (!stuckJobs.isEmpty()) {
            logger.warn("Found {} stuck transcoding jobs, cancelling them", stuckJobs.size());
            
            for (TranscodingJob job : stuckJobs) {
                logger.warn("Cancelling stuck job: {} (started: {})", job.getId(), job.getStartedAt());
                job.cancel();
                jobRepository.save(job);
            }
        }
    }

    /**
     * Scheduled job to clean up old completed jobs
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldJobs() {
        LocalDateTime cleanupThreshold = LocalDateTime.now().minusDays(cleanupDays);
        
        List<TranscodingJob> oldJobs = jobRepository.findJobsForCleanup(cleanupThreshold);
        
        if (!oldJobs.isEmpty()) {
            logger.info("Cleaning up {} old transcoding jobs", oldJobs.size());
            
            for (TranscodingJob job : oldJobs) {
                try {
                    // Delete output files if they exist
                    if (job.getOutputFile() != null) {
                        java.io.File outputFile = new java.io.File(job.getOutputFile());
                        if (outputFile.exists()) {
                            if (outputFile.delete()) {
                                logger.debug("Deleted output file: {}", job.getOutputFile());
                            } else {
                                logger.warn("Failed to delete output file: {}", job.getOutputFile());
                            }
                        }
                    }
                    
                    // Delete job record
                    jobRepository.delete(job);
                    logger.debug("Cleaned up job: {}", job.getId());
                    
                } catch (Exception e) {
                    logger.error("Failed to cleanup job: {}", job.getId(), e);
                }
            }
        }
    }

    /**
     * Get job queue statistics
     */
    public JobQueueStatistics getQueueStatistics() {
        long pending = jobRepository.countPendingJobs();
        long running = jobRepository.countRunningJobs();
        List<Object[]> statusCounts = jobRepository.getJobCountsByStatus();
        List<Object[]> typeCounts = jobRepository.getJobCountsByType();
        Double avgDuration = jobRepository.getAverageJobDurationMinutes();
        
        return new JobQueueStatistics(pending, running, maxConcurrentJobs, statusCounts, 
                                    typeCounts, avgDuration != null ? avgDuration : 0.0);
    }

    /**
     * Cancel all jobs for a specific stream
     */
    public int cancelJobsForStream(StreamSource streamSource) {
        logger.info("Cancelling all jobs for stream: {}", streamSource.getName());
        return jobRepository.cancelJobsForStream(streamSource, LocalDateTime.now());
    }

    // Statistics class
    public static class JobQueueStatistics {
        private final long pendingJobs;
        private final long runningJobs;
        private final int maxConcurrentJobs;
        private final List<Object[]> statusCounts;
        private final List<Object[]> typeCounts;
        private final double averageDurationMinutes;

        public JobQueueStatistics(long pendingJobs, long runningJobs, int maxConcurrentJobs,
                                List<Object[]> statusCounts, List<Object[]> typeCounts, 
                                double averageDurationMinutes) {
            this.pendingJobs = pendingJobs;
            this.runningJobs = runningJobs;
            this.maxConcurrentJobs = maxConcurrentJobs;
            this.statusCounts = statusCounts;
            this.typeCounts = typeCounts;
            this.averageDurationMinutes = averageDurationMinutes;
        }

        public long getPendingJobs() { return pendingJobs; }
        public long getRunningJobs() { return runningJobs; }
        public int getMaxConcurrentJobs() { return maxConcurrentJobs; }
        public List<Object[]> getStatusCounts() { return statusCounts; }
        public List<Object[]> getTypeCounts() { return typeCounts; }
        public double getAverageDurationMinutes() { return averageDurationMinutes; }
        public boolean isAtCapacity() { return runningJobs >= maxConcurrentJobs; }
    }
}