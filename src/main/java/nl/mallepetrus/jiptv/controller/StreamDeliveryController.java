package nl.mallepetrus.jiptv.controller;

import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.TranscodingJob;
import nl.mallepetrus.jiptv.repository.StreamSourceRepository;
import nl.mallepetrus.jiptv.repository.TranscodingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/stream-delivery")
public class StreamDeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(StreamDeliveryController.class);

    private final TranscodingJobRepository jobRepository;
    private final StreamSourceRepository streamSourceRepository;

    @Value("${jiptv.transcoding.output-dir:./transcoded}")
    private String outputDirectory;

    @Autowired
    public StreamDeliveryController(TranscodingJobRepository jobRepository,
                                  StreamSourceRepository streamSourceRepository) {
        this.jobRepository = jobRepository;
        this.streamSourceRepository = streamSourceRepository;
    }

    /**
     * Serve HLS playlist for WebOS TV
     */
    @GetMapping("/hls/{streamId}/playlist.m3u8")
    public ResponseEntity<Resource> getHLSPlaylist(@PathVariable Long streamId) {
        logger.info("Serving HLS playlist for stream: {}", streamId);

        // Find completed HLS job for this stream
        Optional<TranscodingJob> hlsJob = findCompletedHLSJob(streamId);
        if (hlsJob.isEmpty()) {
            logger.warn("No completed HLS job found for stream: {}", streamId);
            return ResponseEntity.notFound().build();
        }

        TranscodingJob job = hlsJob.get();
        File playlistFile = new File(job.getOutputFile());
        
        if (!playlistFile.exists()) {
            logger.warn("HLS playlist file not found: {}", job.getOutputFile());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(playlistFile);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(resource);
    }

    /**
     * Serve HLS segments for WebOS TV
     */
    @GetMapping("/hls/{streamId}/segment_{segmentNumber}.ts")
    public ResponseEntity<Resource> getHLSSegment(@PathVariable Long streamId, 
                                                @PathVariable String segmentNumber) {
        logger.debug("Serving HLS segment for stream: {}, segment: {}", streamId, segmentNumber);

        // Find completed HLS job for this stream
        Optional<TranscodingJob> hlsJob = findCompletedHLSJob(streamId);
        if (hlsJob.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TranscodingJob job = hlsJob.get();
        
        // Build segment file path
        Path playlistPath = Paths.get(job.getOutputFile());
        Path segmentPath = playlistPath.getParent().resolve("segment_" + segmentNumber + ".ts");
        File segmentFile = segmentPath.toFile();
        
        if (!segmentFile.exists()) {
            logger.warn("HLS segment file not found: {}", segmentPath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(segmentFile);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(resource);
    }

    /**
     * Serve transcoded video file for WebOS TV
     */
    @GetMapping("/transcoded/{streamId}")
    public ResponseEntity<Resource> getTranscodedVideo(@PathVariable Long streamId,
                                                     @RequestParam(defaultValue = "WebOS_Compatible") String profile) {
        logger.info("Serving transcoded video for stream: {}, profile: {}", streamId, profile);

        // Find completed transcoding job for this stream and profile
        Optional<TranscodingJob> transcodingJob = findCompletedTranscodingJob(streamId, profile);
        if (transcodingJob.isEmpty()) {
            logger.warn("No completed transcoding job found for stream: {}, profile: {}", streamId, profile);
            return ResponseEntity.notFound().build();
        }

        TranscodingJob job = transcodingJob.get();
        File videoFile = new File(job.getOutputFile());
        
        if (!videoFile.exists()) {
            logger.warn("Transcoded video file not found: {}", job.getOutputFile());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(videoFile);
        
        // Determine content type based on file extension
        String contentType = "video/mp4";
        if (job.getOutputFile().toLowerCase().endsWith(".mkv")) {
            contentType = "video/x-matroska";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(videoFile.length()))
                .body(resource);
    }

    /**
     * Get stream delivery information for WebOS TV
     */
    @GetMapping("/streams/{streamId}/info")
    public ResponseEntity<StreamDeliveryInfo> getStreamDeliveryInfo(@PathVariable Long streamId) {
        logger.info("Getting stream delivery info for stream: {}", streamId);

        StreamSource streamSource = streamSourceRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream source not found: " + streamId));

        StreamDeliveryInfo info = new StreamDeliveryInfo();
        info.setStreamId(streamId);
        info.setStreamName(streamSource.getName());
        info.setOriginalUrl(streamSource.getSourceUrl());

        // Check for available HLS
        Optional<TranscodingJob> hlsJob = findCompletedHLSJob(streamId);
        if (hlsJob.isPresent()) {
            info.setHlsAvailable(true);
            info.setHlsUrl("/api/stream-delivery/hls/" + streamId + "/playlist.m3u8");
        }

        // Check for available transcoded versions
        Optional<TranscodingJob> transcodingJob = findCompletedTranscodingJob(streamId, "WebOS_Compatible");
        if (transcodingJob.isPresent()) {
            info.setTranscodedAvailable(true);
            info.setTranscodedUrl("/api/stream-delivery/transcoded/" + streamId + "?profile=WebOS_Compatible");
        }

        // Determine recommended delivery method
        if (info.isHlsAvailable()) {
            info.setRecommendedDelivery("HLS");
            info.setRecommendedUrl(info.getHlsUrl());
        } else if (info.isTranscodedAvailable()) {
            info.setRecommendedDelivery("TRANSCODED");
            info.setRecommendedUrl(info.getTranscodedUrl());
        } else {
            info.setRecommendedDelivery("ORIGINAL");
            info.setRecommendedUrl(streamSource.getSourceUrl());
        }

        return ResponseEntity.ok(info);
    }

    /**
     * Check stream availability for WebOS TV
     */
    @GetMapping("/streams/{streamId}/availability")
    public ResponseEntity<StreamAvailability> checkStreamAvailability(@PathVariable Long streamId) {
        StreamSource streamSource = streamSourceRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream source not found: " + streamId));

        StreamAvailability availability = new StreamAvailability();
        availability.setStreamId(streamId);
        availability.setOriginalAvailable(true); // Assume original is always available

        // Check HLS availability
        Optional<TranscodingJob> hlsJob = findCompletedHLSJob(streamId);
        availability.setHlsAvailable(hlsJob.isPresent());

        // Check transcoded availability
        Optional<TranscodingJob> transcodingJob = findCompletedTranscodingJob(streamId, "WebOS_Compatible");
        availability.setTranscodedAvailable(transcodingJob.isPresent());

        // Check if transcoding is in progress
        boolean transcodingInProgress = jobRepository.findByStreamSourceIdOrderByCreatedAtDesc(streamId)
                .stream()
                .anyMatch(job -> job.getStatus() == TranscodingJob.Status.RUNNING || 
                               job.getStatus() == TranscodingJob.Status.PENDING);
        availability.setTranscodingInProgress(transcodingInProgress);

        return ResponseEntity.ok(availability);
    }

    // Helper methods
    private Optional<TranscodingJob> findCompletedHLSJob(Long streamId) {
        return jobRepository.findByStreamSourceIdOrderByCreatedAtDesc(streamId)
                .stream()
                .filter(job -> job.getJobType() == TranscodingJob.JobType.SEGMENT)
                .filter(job -> job.getStatus() == TranscodingJob.Status.COMPLETED)
                .filter(job -> job.getOutputFile() != null)
                .findFirst();
    }

    private Optional<TranscodingJob> findCompletedTranscodingJob(Long streamId, String profile) {
        return jobRepository.findByStreamSourceIdOrderByCreatedAtDesc(streamId)
                .stream()
                .filter(job -> job.getJobType() == TranscodingJob.JobType.TRANSCODE)
                .filter(job -> job.getStatus() == TranscodingJob.Status.COMPLETED)
                .filter(job -> profile.equals(job.getTranscodingProfile()))
                .filter(job -> job.getOutputFile() != null)
                .findFirst();
    }

    // Response DTOs
    public static class StreamDeliveryInfo {
        private Long streamId;
        private String streamName;
        private String originalUrl;
        private boolean hlsAvailable;
        private String hlsUrl;
        private boolean transcodedAvailable;
        private String transcodedUrl;
        private String recommendedDelivery;
        private String recommendedUrl;

        // Getters and Setters
        public Long getStreamId() { return streamId; }
        public void setStreamId(Long streamId) { this.streamId = streamId; }
        
        public String getStreamName() { return streamName; }
        public void setStreamName(String streamName) { this.streamName = streamName; }
        
        public String getOriginalUrl() { return originalUrl; }
        public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
        
        public boolean isHlsAvailable() { return hlsAvailable; }
        public void setHlsAvailable(boolean hlsAvailable) { this.hlsAvailable = hlsAvailable; }
        
        public String getHlsUrl() { return hlsUrl; }
        public void setHlsUrl(String hlsUrl) { this.hlsUrl = hlsUrl; }
        
        public boolean isTranscodedAvailable() { return transcodedAvailable; }
        public void setTranscodedAvailable(boolean transcodedAvailable) { this.transcodedAvailable = transcodedAvailable; }
        
        public String getTranscodedUrl() { return transcodedUrl; }
        public void setTranscodedUrl(String transcodedUrl) { this.transcodedUrl = transcodedUrl; }
        
        public String getRecommendedDelivery() { return recommendedDelivery; }
        public void setRecommendedDelivery(String recommendedDelivery) { this.recommendedDelivery = recommendedDelivery; }
        
        public String getRecommendedUrl() { return recommendedUrl; }
        public void setRecommendedUrl(String recommendedUrl) { this.recommendedUrl = recommendedUrl; }
    }

    public static class StreamAvailability {
        private Long streamId;
        private boolean originalAvailable;
        private boolean hlsAvailable;
        private boolean transcodedAvailable;
        private boolean transcodingInProgress;

        // Getters and Setters
        public Long getStreamId() { return streamId; }
        public void setStreamId(Long streamId) { this.streamId = streamId; }
        
        public boolean isOriginalAvailable() { return originalAvailable; }
        public void setOriginalAvailable(boolean originalAvailable) { this.originalAvailable = originalAvailable; }
        
        public boolean isHlsAvailable() { return hlsAvailable; }
        public void setHlsAvailable(boolean hlsAvailable) { this.hlsAvailable = hlsAvailable; }
        
        public boolean isTranscodedAvailable() { return transcodedAvailable; }
        public void setTranscodedAvailable(boolean transcodedAvailable) { this.transcodedAvailable = transcodedAvailable; }
        
        public boolean isTranscodingInProgress() { return transcodingInProgress; }
        public void setTranscodingInProgress(boolean transcodingInProgress) { this.transcodingInProgress = transcodingInProgress; }
    }
}