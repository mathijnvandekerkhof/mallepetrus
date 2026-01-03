package nl.mallepetrus.jiptv.service;

import nl.mallepetrus.jiptv.entity.*;
import nl.mallepetrus.jiptv.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class StreamManagementService {

    private static final Logger logger = LoggerFactory.getLogger(StreamManagementService.class);

    private final StreamSourceRepository streamSourceRepository;
    private final StreamTrackRepository streamTrackRepository;
    private final UserStreamPreferencesRepository preferencesRepository;
    private final StreamSessionRepository sessionRepository;
    private final TranscodingJobRepository jobRepository;
    private final FFmpegService ffmpegService;
    private final RedisTemplate<String, Object> streamMetadataRedis;
    private final RedisTemplate<String, Object> streamUrlRedis;

    // Cache TTL constants
    private static final long STREAM_METADATA_TTL_HOURS = 24;
    private static final long USER_PREFERENCES_TTL_HOURS = 1;
    private static final long STREAM_URL_TTL_MINUTES = 30;

    @Autowired
    public StreamManagementService(
            StreamSourceRepository streamSourceRepository,
            StreamTrackRepository streamTrackRepository,
            UserStreamPreferencesRepository preferencesRepository,
            StreamSessionRepository sessionRepository,
            TranscodingJobRepository jobRepository,
            FFmpegService ffmpegService,
            @Qualifier("streamMetadataRedisTemplate") RedisTemplate<String, Object> streamMetadataRedis,
            @Qualifier("streamUrlRedisTemplate") RedisTemplate<String, Object> streamUrlRedis) {
        
        this.streamSourceRepository = streamSourceRepository;
        this.streamTrackRepository = streamTrackRepository;
        this.preferencesRepository = preferencesRepository;
        this.sessionRepository = sessionRepository;
        this.jobRepository = jobRepository;
        this.ffmpegService = ffmpegService;
        this.streamMetadataRedis = streamMetadataRedis;
        this.streamUrlRedis = streamUrlRedis;
    }

    /**
     * Add a new stream source and analyze it
     */
    public StreamSource addStreamSource(String name, String description, 
                                       StreamSource.SourceType sourceType, String sourceUrl) {
        logger.info("Adding new stream source: {} ({})", name, sourceType);
        
        // Check for duplicate file hash if it's a file
        if (sourceType == StreamSource.SourceType.FILE) {
            // This would be calculated during analysis
        }
        
        StreamSource streamSource = new StreamSource(name, sourceType, sourceUrl);
        streamSource.setDescription(description);
        streamSource = streamSourceRepository.save(streamSource);
        
        // Schedule analysis job
        scheduleAnalysisJob(streamSource);
        
        return streamSource;
    }

    /**
     * Analyze stream source using FFmpeg/FFprobe
     */
    public void analyzeStreamSource(Long streamSourceId) {
        Optional<StreamSource> optionalSource = streamSourceRepository.findById(streamSourceId);
        if (optionalSource.isEmpty()) {
            throw new RuntimeException("Stream source not found: " + streamSourceId);
        }
        
        StreamSource streamSource = optionalSource.get();
        logger.info("Analyzing stream source: {}", streamSource.getName());
        
        try {
            // Clear existing tracks if re-analyzing
            if (streamSource.isAnalyzed()) {
                streamTrackRepository.deleteByStreamSource(streamSource);
                clearStreamCache(streamSourceId);
            }
            
            // Perform FFprobe analysis
            FFmpegService.StreamAnalysisResult result = ffmpegService.analyzeStream(streamSource);
            
            if (result.isAnalysisSuccessful()) {
                // Save tracks to database
                for (StreamTrack track : result.getTracks()) {
                    streamTrackRepository.save(track);
                }
                
                // Update stream source
                streamSource.setAnalyzedAt(LocalDateTime.now());
                streamSourceRepository.save(streamSource);
                
                // Cache stream metadata - DISABLED FOR NOW
                // cacheStreamMetadata(streamSource, result.getTracks());
                
                logger.info("Successfully analyzed stream: {} - {} tracks found", 
                           streamSource.getName(), result.getTracks().size());
            } else {
                throw new RuntimeException("Stream analysis failed: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("Failed to analyze stream source: {}", streamSource.getName(), e);
            throw new RuntimeException("Stream analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get stream source with tracks (cached)
     */
    public Optional<StreamSource> getStreamWithTracks(Long streamSourceId) {
        String cacheKey = "stream:metadata:" + streamSourceId;
        
        // Try cache first
        StreamSource cached = (StreamSource) streamMetadataRedis.opsForValue().get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Load from database
        Optional<StreamSource> streamSource = streamSourceRepository.findByIdWithTracks(streamSourceId);
        if (streamSource.isPresent()) {
            // Cache for future requests
            streamMetadataRedis.opsForValue().set(cacheKey, streamSource.get(), 
                                                 STREAM_METADATA_TTL_HOURS, TimeUnit.HOURS);
        }
        
        return streamSource;
    }

    /**
     * Get or create user preferences for a stream
     */
    public UserStreamPreferences getUserStreamPreferences(Long userId, Long streamSourceId) {
        String cacheKey = "user:" + userId + ":stream:" + streamSourceId + ":prefs";
        
        // Try cache first - DISABLED FOR NOW
        // UserStreamPreferences cached = (UserStreamPreferences) streamMetadataRedis.opsForValue().get(cacheKey);
        // if (cached != null) {
        //     return cached;
        // }
        
        // Load from database or create new
        Optional<UserStreamPreferences> existing = preferencesRepository.findByUserIdAndStreamSourceId(userId, streamSourceId);
        UserStreamPreferences preferences;
        
        if (existing.isPresent()) {
            preferences = existing.get();
        } else {
            // Create default preferences
            User user = new User();
            user.setId(userId);
            StreamSource streamSource = new StreamSource();
            streamSource.setId(streamSourceId);
            
            preferences = new UserStreamPreferences(user, streamSource);
            preferences = preferencesRepository.save(preferences);
        }
        
        // Cache preferences - DISABLED FOR NOW
        // streamMetadataRedis.opsForValue().set(cacheKey, preferences, 
        //                                      USER_PREFERENCES_TTL_HOURS, TimeUnit.HOURS);
        
        return preferences;
    }

    /**
     * Update user stream preferences
     */
    public UserStreamPreferences updateUserStreamPreferences(Long userId, Long streamSourceId,
                                                           Long videoTrackId, Long audioTrackId, 
                                                           Long subtitleTrackId, boolean subtitleEnabled,
                                                           UserStreamPreferences.Quality quality) {
        
        UserStreamPreferences preferences = getUserStreamPreferences(userId, streamSourceId);
        
        // Update track preferences
        if (videoTrackId != null) {
            StreamTrack videoTrack = streamTrackRepository.findById(videoTrackId).orElse(null);
            preferences.setPreferredVideoTrack(videoTrack);
        }
        
        if (audioTrackId != null) {
            StreamTrack audioTrack = streamTrackRepository.findById(audioTrackId).orElse(null);
            preferences.setPreferredAudioTrack(audioTrack);
        }
        
        if (subtitleTrackId != null) {
            StreamTrack subtitleTrack = streamTrackRepository.findById(subtitleTrackId).orElse(null);
            preferences.setPreferredSubtitleTrack(subtitleTrack);
        }
        
        preferences.setSubtitleEnabled(subtitleEnabled);
        preferences.setPreferredQuality(quality);
        
        preferences = preferencesRepository.save(preferences);
        
        // Update cache - DISABLED FOR NOW
        // String cacheKey = "user:" + userId + ":stream:" + streamSourceId + ":prefs";
        // streamMetadataRedis.opsForValue().set(cacheKey, preferences, 
        //                                      USER_PREFERENCES_TTL_HOURS, TimeUnit.HOURS);
        
        return preferences;
    }

    /**
     * Get available tracks for a stream by type
     */
    public List<StreamTrack> getTracksByType(Long streamSourceId, StreamTrack.TrackType trackType) {
        return streamTrackRepository.findByStreamSourceIdAndTrackTypeOrderByTrackIndexAsc(streamSourceId, trackType);
    }

    /**
     * Get recommended tracks for WebOS TV compatibility
     */
    public RecommendedTracks getRecommendedTracks(Long streamSourceId) {
        Optional<StreamTrack> bestVideo = streamTrackRepository.findBestVideoTrack(
                streamSourceRepository.findById(streamSourceId).orElse(null));
        Optional<StreamTrack> bestAudio = streamTrackRepository.findBestAudioTrack(
                streamSourceRepository.findById(streamSourceId).orElse(null));
        
        return new RecommendedTracks(bestVideo.orElse(null), bestAudio.orElse(null));
    }

    /**
     * Schedule FFprobe analysis job
     */
    private void scheduleAnalysisJob(StreamSource streamSource) {
        TranscodingJob analysisJob = new TranscodingJob(streamSource, 
                                                       TranscodingJob.JobType.ANALYSIS, 
                                                       streamSource.getSourceUrl());
        jobRepository.save(analysisJob);
        
        logger.info("Scheduled analysis job for stream: {}", streamSource.getName());
    }

    /**
     * Cache stream metadata in Redis
     */
    private void cacheStreamMetadata(StreamSource streamSource, List<StreamTrack> tracks) {
        String cacheKey = "stream:metadata:" + streamSource.getId();
        
        // Set tracks on stream source for caching
        streamSource.setTracks(tracks);
        
        streamMetadataRedis.opsForValue().set(cacheKey, streamSource, 
                                             STREAM_METADATA_TTL_HOURS, TimeUnit.HOURS);
        
        logger.debug("Cached metadata for stream: {}", streamSource.getName());
    }

    /**
     * Clear stream cache
     */
    private void clearStreamCache(Long streamSourceId) {
        String metadataKey = "stream:metadata:" + streamSourceId;
        streamMetadataRedis.delete(metadataKey);
        
        // Clear user preferences cache for this stream
        String prefsPattern = "user:*:stream:" + streamSourceId + ":prefs";
        // Note: In production, you'd want a more efficient way to clear pattern-based keys
        
        logger.debug("Cleared cache for stream: {}", streamSourceId);
    }

    /**
     * Get stream sources that need analysis
     */
    public List<StreamSource> getSourcesNeedingAnalysis() {
        return streamSourceRepository.findSourcesNeedingAnalysis();
    }

    /**
     * Check if FFmpeg is available
     */
    public boolean isFFmpegAvailable() {
        return ffmpegService.isFFmpegAvailable();
    }

    /**
     * Get stream statistics
     */
    public StreamStatistics getStreamStatistics() {
        long totalSources = streamSourceRepository.countActiveSources();
        long analyzedSources = streamSourceRepository.countAnalyzedSources();
        long activeSessions = sessionRepository.countActiveSessions();
        
        return new StreamStatistics(totalSources, analyzedSources, activeSessions);
    }

    /**
     * Get stream source by ID
     */
    public StreamSource getStreamSourceById(Long streamSourceId) {
        return streamSourceRepository.findById(streamSourceId)
                .orElseThrow(() -> new RuntimeException("Stream source not found: " + streamSourceId));
    }

    /**
     * Get stream tracks by IDs
     */
    public List<StreamTrack> getStreamTracksByIds(List<Long> trackIds) {
        return streamTrackRepository.findAllById(trackIds);
    }

    /**
     * Get transcoding jobs for a stream
     */
    public List<TranscodingJob> getTranscodingJobsForStream(Long streamSourceId) {
        return jobRepository.findByStreamSourceIdOrderByCreatedAtDesc(streamSourceId);
    }

    // Helper classes
    public static class RecommendedTracks {
        private final StreamTrack videoTrack;
        private final StreamTrack audioTrack;

        public RecommendedTracks(StreamTrack videoTrack, StreamTrack audioTrack) {
            this.videoTrack = videoTrack;
            this.audioTrack = audioTrack;
        }

        public StreamTrack getVideoTrack() { return videoTrack; }
        public StreamTrack getAudioTrack() { return audioTrack; }
    }

    public static class StreamStatistics {
        private final long totalSources;
        private final long analyzedSources;
        private final long activeSessions;

        public StreamStatistics(long totalSources, long analyzedSources, long activeSessions) {
            this.totalSources = totalSources;
            this.analyzedSources = analyzedSources;
            this.activeSessions = activeSessions;
        }

        public long getTotalSources() { return totalSources; }
        public long getAnalyzedSources() { return analyzedSources; }
        public long getActiveSessions() { return activeSessions; }
        public double getAnalysisProgress() { 
            return totalSources > 0 ? (double) analyzedSources / totalSources * 100 : 0; 
        }
    }
}