package nl.mallepetrus.jiptv.controller;

import jakarta.validation.Valid;
import nl.mallepetrus.jiptv.dto.*;
import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.StreamTrack;
import nl.mallepetrus.jiptv.entity.TranscodingJob;
import nl.mallepetrus.jiptv.entity.UserStreamPreferences;
import nl.mallepetrus.jiptv.security.UserPrincipal;
import nl.mallepetrus.jiptv.service.StreamManagementService;
import nl.mallepetrus.jiptv.service.TranscodingJobQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/streams")
public class StreamManagementController {

    private static final Logger logger = LoggerFactory.getLogger(StreamManagementController.class);

    private final StreamManagementService streamManagementService;
    private final TranscodingJobQueueService transcodingJobQueueService;

    @Autowired
    public StreamManagementController(StreamManagementService streamManagementService,
                                    TranscodingJobQueueService transcodingJobQueueService) {
        this.streamManagementService = streamManagementService;
        this.transcodingJobQueueService = transcodingJobQueueService;
    }

    /**
     * Add a new stream source (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StreamSourceResponse> addStreamSource(
            @Valid @RequestBody AddStreamSourceRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Admin {} adding new stream source: {}", userPrincipal.getUsername(), request.getName());
        
        try {
            StreamSource.SourceType sourceType = StreamSource.SourceType.valueOf(request.getSourceType().toUpperCase());
            
            StreamSource streamSource = streamManagementService.addStreamSource(
                    request.getName(),
                    request.getDescription(),
                    sourceType,
                    request.getSourceUrl()
            );
            
            return ResponseEntity.ok(new StreamSourceResponse(streamSource));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to add stream source", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all stream sources
     */
    @GetMapping
    public ResponseEntity<List<StreamSourceResponse>> getAllStreamSources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            // For now, return all sources - in production you might want pagination
            List<StreamSource> sources = streamManagementService.getSourcesNeedingAnalysis();
            
            List<StreamSourceResponse> response = sources.stream()
                    .map(StreamSourceResponse::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get stream sources", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simple test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Streaming API is working", "timestamp", LocalDateTime.now().toString()));
    }

    /**
     * Get stream source by ID with tracks
     */
    @GetMapping("/{streamId}")
    public ResponseEntity<StreamSourceResponse> getStreamSource(@PathVariable Long streamId) {
        try {
            Optional<StreamSource> streamSource = streamManagementService.getStreamWithTracks(streamId);
            
            if (streamSource.isPresent()) {
                return ResponseEntity.ok(new StreamSourceResponse(streamSource.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get stream source: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Analyze stream source (Admin only)
     */
    @PostMapping("/{streamId}/analyze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> analyzeStream(
            @PathVariable Long streamId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Admin {} requesting analysis for stream: {}", userPrincipal.getUsername(), streamId);
        
        try {
            streamManagementService.analyzeStreamSource(streamId);
            return ResponseEntity.ok(Map.of("message", "Stream analysis completed successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to analyze stream: {}", streamId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get tracks for a stream by type
     */
    @GetMapping("/{streamId}/tracks")
    public ResponseEntity<List<StreamTrackResponse>> getStreamTracks(
            @PathVariable Long streamId,
            @RequestParam(required = false) String type) {
        
        try {
            List<StreamTrack> tracks;
            
            if (type != null) {
                StreamTrack.TrackType trackType = StreamTrack.TrackType.valueOf(type.toUpperCase());
                tracks = streamManagementService.getTracksByType(streamId, trackType);
            } else {
                Optional<StreamSource> streamSource = streamManagementService.getStreamWithTracks(streamId);
                if (streamSource.isPresent()) {
                    tracks = streamSource.get().getTracks();
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            
            List<StreamTrackResponse> response = tracks.stream()
                    .map(StreamTrackResponse::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to get tracks for stream: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get recommended tracks for WebOS TV
     */
    @GetMapping("/{streamId}/recommended-tracks")
    public ResponseEntity<Map<String, StreamTrackResponse>> getRecommendedTracks(@PathVariable Long streamId) {
        try {
            StreamManagementService.RecommendedTracks recommended = 
                    streamManagementService.getRecommendedTracks(streamId);
            
            Map<String, StreamTrackResponse> response = Map.of(
                    "video", recommended.getVideoTrack() != null ? 
                            new StreamTrackResponse(recommended.getVideoTrack()) : null,
                    "audio", recommended.getAudioTrack() != null ? 
                            new StreamTrackResponse(recommended.getAudioTrack()) : null
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get recommended tracks for stream: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user preferences for a stream
     */
    @GetMapping("/{streamId}/preferences")
    public ResponseEntity<UserStreamPreferencesResponse> getUserPreferences(
            @PathVariable Long streamId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            UserStreamPreferences preferences = streamManagementService.getUserStreamPreferences(
                    userPrincipal.getId(), streamId);
            
            return ResponseEntity.ok(new UserStreamPreferencesResponse(preferences));
            
        } catch (Exception e) {
            logger.error("Failed to get user preferences for stream: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update user preferences for a stream
     */
    @PutMapping("/{streamId}/preferences")
    public ResponseEntity<UserStreamPreferencesResponse> updateUserPreferences(
            @PathVariable Long streamId,
            @Valid @RequestBody UpdateStreamPreferencesRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            UserStreamPreferences.Quality quality = UserStreamPreferences.Quality.valueOf(
                    request.getQuality().toUpperCase());
            
            UserStreamPreferences preferences = streamManagementService.updateUserStreamPreferences(
                    userPrincipal.getId(),
                    streamId,
                    request.getVideoTrackId(),
                    request.getAudioTrackId(),
                    request.getSubtitleTrackId(),
                    request.isSubtitleEnabled(),
                    quality
            );
            
            return ResponseEntity.ok(new UserStreamPreferencesResponse(preferences));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to update user preferences for stream: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get stream statistics (Admin only)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StreamManagementService.StreamStatistics> getStreamStatistics() {
        try {
            StreamManagementService.StreamStatistics stats = streamManagementService.getStreamStatistics();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Failed to get stream statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check FFmpeg availability (Admin only)
     */
    @GetMapping("/ffmpeg-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFFmpegStatus() {
        try {
            boolean available = streamManagementService.isFFmpegAvailable();
            return ResponseEntity.ok(Map.of(
                    "available", available,
                    "message", available ? "FFmpeg is available" : "FFmpeg is not available or not configured"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to check FFmpeg status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Start transcoding job for WebOS compatibility (Admin only)
     */
    @PostMapping("/{streamId}/transcode")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TranscodingJobResponse> startTranscodingJob(
            @PathVariable Long streamId,
            @Valid @RequestBody TranscodingJobRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Admin {} starting transcoding job for stream: {}", userPrincipal.getUsername(), streamId);
        
        try {
            // Set the stream ID from path parameter
            request.setStreamSourceId(streamId);
            
            // Get stream source and selected tracks
            StreamSource streamSource = streamManagementService.getStreamSourceById(streamId);
            List<StreamTrack> selectedTracks = streamManagementService.getStreamTracksByIds(request.getSelectedTrackIds());
            
            // Validate tracks belong to the stream
            boolean allTracksValid = selectedTracks.stream()
                    .allMatch(track -> track.getStreamSource().getId().equals(streamId));
            if (!allTracksValid) {
                return ResponseEntity.badRequest().build();
            }
            
            // Queue transcoding job
            TranscodingJob job;
            if (request.isGenerateHLS()) {
                job = transcodingJobQueueService.queueHLSJob(streamSource, selectedTracks);
            } else {
                job = transcodingJobQueueService.queueTranscodingJob(streamSource, selectedTracks);
            }
            
            return ResponseEntity.ok(new TranscodingJobResponse(job));
            
        } catch (Exception e) {
            logger.error("Failed to start transcoding job for stream: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get transcoding jobs for a stream
     */
    @GetMapping("/{streamId}/transcoding-jobs")
    public ResponseEntity<List<TranscodingJobResponse>> getTranscodingJobs(@PathVariable Long streamId) {
        try {
            List<TranscodingJob> jobs = streamManagementService.getTranscodingJobsForStream(streamId);
            List<TranscodingJobResponse> response = jobs.stream()
                    .map(TranscodingJobResponse::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get transcoding jobs for stream: {}", streamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}