package nl.mallepetrus.jiptv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.StreamTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class FFmpegService {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jiptv.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${jiptv.ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Value("${jiptv.ffmpeg.timeout:300}")
    private int ffmpegTimeoutSeconds;

    // WebOS TV compatible codecs
    private static final List<String> WEBOS_VIDEO_CODECS = Arrays.asList(
            "h264", "hevc", "h265", "vp9"
    );

    private static final List<String> WEBOS_AUDIO_CODECS = Arrays.asList(
            "aac", "ac3", "mp3", "pcm_s16le", "pcm_s24le"
    );

    // Audio codecs that need transcoding for WebOS
    private static final List<String> TRANSCODING_REQUIRED_AUDIO = Arrays.asList(
            "dts", "truehd", "eac3", "dts-hd", "mlp", "flac"
    );

    /**
     * Analyze stream using FFprobe to extract track information
     */
    public StreamAnalysisResult analyzeStream(StreamSource streamSource) {
        logger.info("Starting FFprobe analysis for stream: {}", streamSource.getName());
        
        try {
            // Build FFprobe command
            List<String> command = buildFFprobeCommand(streamSource.getSourceUrl());
            
            // Execute FFprobe
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(ffmpegTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFprobe analysis timed out after " + ffmpegTimeoutSeconds + " seconds");
            }
            
            if (process.exitValue() != 0) {
                throw new RuntimeException("FFprobe failed with exit code: " + process.exitValue() + 
                                         "\nOutput: " + output.toString());
            }
            
            // Parse FFprobe JSON output
            return parseFFprobeOutput(streamSource, output.toString());
            
        } catch (Exception e) {
            logger.error("FFprobe analysis failed for stream: {}", streamSource.getName(), e);
            throw new RuntimeException("Stream analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build FFprobe command for stream analysis
     */
    private List<String> buildFFprobeCommand(String sourceUrl) {
        return Arrays.asList(
                ffprobePath,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                "-show_chapters",
                sourceUrl
        );
    }

    /**
     * Parse FFprobe JSON output and create StreamTrack entities
     */
    private StreamAnalysisResult parseFFprobeOutput(StreamSource streamSource, String jsonOutput) {
        try {
            JsonNode root = objectMapper.readTree(jsonOutput);
            StreamAnalysisResult result = new StreamAnalysisResult();
            
            // Parse format information
            JsonNode format = root.get("format");
            if (format != null) {
                updateStreamSourceFromFormat(streamSource, format);
            }
            
            // Parse streams (tracks)
            JsonNode streams = root.get("streams");
            if (streams != null && streams.isArray()) {
                for (int i = 0; i < streams.size(); i++) {
                    JsonNode stream = streams.get(i);
                    StreamTrack track = parseStreamTrack(streamSource, stream, i);
                    if (track != null) {
                        result.addTrack(track);
                    }
                }
            }
            
            result.setAnalysisSuccessful(true);
            logger.info("Successfully analyzed stream: {} - Found {} tracks", 
                       streamSource.getName(), result.getTracks().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to parse FFprobe output", e);
            throw new RuntimeException("Failed to parse stream analysis results: " + e.getMessage(), e);
        }
    }

    /**
     * Update StreamSource with format information from FFprobe
     */
    private void updateStreamSourceFromFormat(StreamSource streamSource, JsonNode format) {
        // Duration
        if (format.has("duration")) {
            double duration = format.get("duration").asDouble();
            streamSource.setDurationSeconds((int) Math.round(duration));
        }
        
        // File size
        if (format.has("size")) {
            streamSource.setFileSize(format.get("size").asLong());
        }
        
        // Format name
        if (format.has("format_name")) {
            String formatName = format.get("format_name").asText();
            streamSource.setContentType(formatName);
        }
        
        // Calculate file hash if it's a local file
        if (streamSource.getSourceType() == StreamSource.SourceType.FILE) {
            try {
                String hash = calculateFileHash(streamSource.getSourceUrl());
                streamSource.setFileHash(hash);
            } catch (Exception e) {
                logger.warn("Failed to calculate file hash for: {}", streamSource.getSourceUrl(), e);
            }
        }
        
        streamSource.setAnalyzedAt(LocalDateTime.now());
    }

    /**
     * Parse individual stream track from FFprobe output
     */
    private StreamTrack parseStreamTrack(StreamSource streamSource, JsonNode stream, int index) {
        String codecType = stream.get("codec_type").asText();
        
        StreamTrack.TrackType trackType;
        switch (codecType.toLowerCase()) {
            case "video":
                trackType = StreamTrack.TrackType.VIDEO;
                break;
            case "audio":
                trackType = StreamTrack.TrackType.AUDIO;
                break;
            case "subtitle":
                trackType = StreamTrack.TrackType.SUBTITLE;
                break;
            default:
                logger.debug("Skipping unsupported track type: {}", codecType);
                return null;
        }
        
        StreamTrack track = new StreamTrack(streamSource, index, trackType);
        
        // Basic track information
        if (stream.has("codec_name")) {
            track.setCodecName(stream.get("codec_name").asText());
        }
        
        if (stream.has("codec_long_name")) {
            track.setCodecLongName(stream.get("codec_long_name").asText());
        }
        
        // Parse tags for language and title
        JsonNode tags = stream.get("tags");
        if (tags != null) {
            if (tags.has("language")) {
                track.setLanguage(tags.get("language").asText());
            }
            if (tags.has("title")) {
                track.setTitle(tags.get("title").asText());
            }
        }
        
        // Disposition (default, forced flags)
        JsonNode disposition = stream.get("disposition");
        if (disposition != null) {
            track.setDefault(disposition.get("default").asInt(0) == 1);
            track.setForced(disposition.get("forced").asInt(0) == 1);
        }
        
        // Duration
        if (stream.has("duration")) {
            track.setDurationSeconds(new BigDecimal(stream.get("duration").asText()));
        }
        
        // Bitrate
        if (stream.has("bit_rate")) {
            track.setBitrate(stream.get("bit_rate").asLong());
        }
        
        // Track type specific parsing
        switch (trackType) {
            case VIDEO:
                parseVideoTrackDetails(track, stream);
                break;
            case AUDIO:
                parseAudioTrackDetails(track, stream);
                break;
            case SUBTITLE:
                parseSubtitleTrackDetails(track, stream);
                break;
        }
        
        // Determine WebOS compatibility and transcoding requirements
        determineCompatibility(track);
        
        return track;
    }

    /**
     * Parse video-specific track details
     */
    private void parseVideoTrackDetails(StreamTrack track, JsonNode stream) {
        if (stream.has("width")) {
            track.setWidth(stream.get("width").asInt());
        }
        
        if (stream.has("height")) {
            track.setHeight(stream.get("height").asInt());
        }
        
        if (stream.has("r_frame_rate")) {
            track.setFrameRate(stream.get("r_frame_rate").asText());
        }
        
        if (stream.has("pix_fmt")) {
            track.setPixelFormat(stream.get("pix_fmt").asText());
        }
        
        if (stream.has("color_space")) {
            track.setColorSpace(stream.get("color_space").asText());
        }
        
        // HDR metadata detection
        if (stream.has("color_primaries") || stream.has("color_trc") || 
            stream.has("side_data_list")) {
            // Simplified HDR detection - in production, this would be more sophisticated
            JsonNode sideData = stream.get("side_data_list");
            if (sideData != null && sideData.isArray()) {
                for (JsonNode data : sideData) {
                    if (data.has("side_data_type")) {
                        String sideDataType = data.get("side_data_type").asText();
                        if (sideDataType.contains("HDR") || sideDataType.contains("mastering")) {
                            track.setHdrMetadata(data.toString());
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse audio-specific track details
     */
    private void parseAudioTrackDetails(StreamTrack track, JsonNode stream) {
        if (stream.has("channels")) {
            track.setChannels(stream.get("channels").asInt());
        }
        
        if (stream.has("channel_layout")) {
            track.setChannelLayout(stream.get("channel_layout").asText());
        }
        
        if (stream.has("sample_rate")) {
            track.setSampleRate(stream.get("sample_rate").asInt());
        }
        
        if (stream.has("bits_per_sample")) {
            track.setBitDepth(stream.get("bits_per_sample").asInt());
        }
    }

    /**
     * Parse subtitle-specific track details
     */
    private void parseSubtitleTrackDetails(StreamTrack track, JsonNode stream) {
        // Subtitle tracks mainly use the basic track information
        // Additional subtitle-specific parsing can be added here if needed
    }

    /**
     * Determine WebOS compatibility and transcoding requirements
     */
    private void determineCompatibility(StreamTrack track) {
        String codecName = track.getCodecName();
        if (codecName == null) {
            track.setWebosCompatible(false);
            track.setTranscodingRequired(true);
            return;
        }
        
        codecName = codecName.toLowerCase();
        
        switch (track.getTrackType()) {
            case VIDEO:
                track.setWebosCompatible(WEBOS_VIDEO_CODECS.contains(codecName));
                track.setTranscodingRequired(!track.isWebosCompatible());
                break;
                
            case AUDIO:
                track.setWebosCompatible(WEBOS_AUDIO_CODECS.contains(codecName));
                track.setTranscodingRequired(TRANSCODING_REQUIRED_AUDIO.contains(codecName));
                break;
                
            case SUBTITLE:
                // Most subtitle formats need conversion to WebVTT for WebOS
                track.setWebosCompatible(codecName.equals("webvtt") || codecName.equals("srt"));
                track.setTranscodingRequired(!track.isWebosCompatible());
                break;
        }
    }

    /**
     * Calculate SHA-256 hash of a file
     */
    private String calculateFileHash(String filePath) throws IOException, NoSuchAlgorithmException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] hashBytes = digest.digest(fileBytes);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    /**
     * Check if FFmpeg/FFprobe is available
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobePath, "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            logger.warn("FFprobe not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Result class for stream analysis
     */
    public static class StreamAnalysisResult {
        private final List<StreamTrack> tracks = new ArrayList<>();
        private boolean analysisSuccessful = false;
        private String errorMessage;

        public void addTrack(StreamTrack track) {
            tracks.add(track);
        }

        public List<StreamTrack> getTracks() {
            return tracks;
        }

        public boolean isAnalysisSuccessful() {
            return analysisSuccessful;
        }

        public void setAnalysisSuccessful(boolean analysisSuccessful) {
            this.analysisSuccessful = analysisSuccessful;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public int getVideoTrackCount() {
            return (int) tracks.stream().filter(t -> t.getTrackType() == StreamTrack.TrackType.VIDEO).count();
        }

        public int getAudioTrackCount() {
            return (int) tracks.stream().filter(t -> t.getTrackType() == StreamTrack.TrackType.AUDIO).count();
        }

        public int getSubtitleTrackCount() {
            return (int) tracks.stream().filter(t -> t.getTrackType() == StreamTrack.TrackType.SUBTITLE).count();
        }

        public boolean hasWebosIncompatibleTracks() {
            return tracks.stream().anyMatch(t -> !t.isWebosCompatible());
        }

        public boolean requiresTranscoding() {
            return tracks.stream().anyMatch(StreamTrack::isTranscodingRequired);
        }
    }
}