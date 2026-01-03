package nl.mallepetrus.jiptv.service;

import nl.mallepetrus.jiptv.entity.*;
import nl.mallepetrus.jiptv.repository.TranscodingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FFmpegTranscodingService {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegTranscodingService.class);

    @Value("${jiptv.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${jiptv.transcoding.output-dir:./transcoded}")
    private String outputDirectory;

    @Value("${jiptv.transcoding.hls-segment-duration:6}")
    private int hlsSegmentDuration;

    @Value("${jiptv.transcoding.timeout:3600}")
    private int transcodingTimeoutSeconds;

    private final TranscodingJobRepository jobRepository;

    // Progress parsing patterns
    private static final Pattern FRAME_PATTERN = Pattern.compile("frame=\\s*(\\d+)");
    private static final Pattern TIME_PATTERN = Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");
    private static final Pattern SPEED_PATTERN = Pattern.compile("speed=\\s*([\\d.]+)x");

    @Autowired
    public FFmpegTranscodingService(TranscodingJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Start transcoding job for WebOS TV compatibility
     */
    public TranscodingJob startTranscodingJob(StreamSource streamSource, List<StreamTrack> selectedTracks, 
                                            TranscodingProfile profile) {
        logger.info("Starting transcoding job for stream: {} with profile: {}", 
                   streamSource.getName(), profile.getName());

        // Create transcoding job
        TranscodingJob job = new TranscodingJob(streamSource, TranscodingJob.JobType.TRANSCODE, 
                                              streamSource.getSourceUrl());
        job.setTranscodingProfile(profile.getName());
        job.setTargetTracks(createTargetTracksJson(selectedTracks));
        
        // Generate output paths
        String outputPath = generateOutputPath(streamSource, profile);
        job.setOutputFile(outputPath);
        
        // Build FFmpeg command
        List<String> command = buildTranscodingCommand(streamSource, selectedTracks, profile, outputPath);
        job.setFfmpegCommand(String.join(" ", command));
        
        job = jobRepository.save(job);
        
        // Start transcoding in background thread
        startTranscodingProcess(job, command);
        
        return job;
    }

    /**
     * Generate HLS segments for adaptive streaming
     */
    public TranscodingJob generateHLSSegments(StreamSource streamSource, List<StreamTrack> selectedTracks,
                                            HLSProfile hlsProfile) {
        logger.info("Generating HLS segments for stream: {} with profile: {}", 
                   streamSource.getName(), hlsProfile.getName());

        TranscodingJob job = new TranscodingJob(streamSource, TranscodingJob.JobType.SEGMENT, 
                                              streamSource.getSourceUrl());
        job.setTranscodingProfile(hlsProfile.getName());
        job.setTargetTracks(createTargetTracksJson(selectedTracks));
        
        // Generate HLS output directory
        String hlsOutputDir = generateHLSOutputPath(streamSource, hlsProfile);
        job.setOutputFile(hlsOutputDir + "/playlist.m3u8");
        
        // Build HLS FFmpeg command
        List<String> command = buildHLSCommand(streamSource, selectedTracks, hlsProfile, hlsOutputDir);
        job.setFfmpegCommand(String.join(" ", command));
        
        job = jobRepository.save(job);
        
        // Start HLS generation in background thread
        startTranscodingProcess(job, command);
        
        return job;
    }

    /**
     * Build FFmpeg command for transcoding
     */
    private List<String> buildTranscodingCommand(StreamSource streamSource, List<StreamTrack> selectedTracks,
                                               TranscodingProfile profile, String outputPath) {
        List<String> command = new ArrayList<>();
        
        // Basic FFmpeg command
        command.add(ffmpegPath);
        command.add("-i");
        command.add(streamSource.getSourceUrl());
        
        // Video encoding settings
        StreamTrack videoTrack = selectedTracks.stream()
                .filter(t -> t.getTrackType() == StreamTrack.TrackType.VIDEO)
                .findFirst().orElse(null);
        
        if (videoTrack != null) {
            if (videoTrack.isWebosCompatible() && !profile.isForceReencode()) {
                // Copy video if compatible
                command.add("-c:v");
                command.add("copy");
            } else {
                // Transcode video
                command.add("-c:v");
                command.add(profile.getVideoCodec());
                command.add("-preset");
                command.add(profile.getVideoPreset());
                command.add("-crf");
                command.add(String.valueOf(profile.getVideoCrf()));
                
                // Resolution scaling if needed
                if (profile.getMaxWidth() > 0 && videoTrack.getWidth() > profile.getMaxWidth()) {
                    command.add("-vf");
                    command.add(String.format("scale=%d:-2", profile.getMaxWidth()));
                }
            }
        }
        
        // Audio encoding settings
        StreamTrack audioTrack = selectedTracks.stream()
                .filter(t -> t.getTrackType() == StreamTrack.TrackType.AUDIO)
                .findFirst().orElse(null);
        
        if (audioTrack != null) {
            if (audioTrack.isWebosCompatible() && !profile.isForceReencode()) {
                // Copy audio if compatible
                command.add("-c:a");
                command.add("copy");
            } else {
                // Transcode audio for WebOS compatibility
                command.add("-c:a");
                command.add(profile.getAudioCodec());
                command.add("-b:a");
                command.add(profile.getAudioBitrate() + "k");
                command.add("-ac");
                command.add(String.valueOf(Math.min(audioTrack.getChannels(), profile.getMaxAudioChannels())));
            }
        }
        
        // Subtitle handling
        StreamTrack subtitleTrack = selectedTracks.stream()
                .filter(t -> t.getTrackType() == StreamTrack.TrackType.SUBTITLE)
                .findFirst().orElse(null);
        
        if (subtitleTrack != null) {
            if (subtitleTrack.isWebosCompatible()) {
                command.add("-c:s");
                command.add("copy");
            } else {
                // Convert to WebVTT for WebOS
                command.add("-c:s");
                command.add("webvtt");
            }
        }
        
        // Output format
        command.add("-f");
        command.add(profile.getOutputFormat());
        
        // Progress reporting
        command.add("-progress");
        command.add("pipe:1");
        
        // Overwrite output
        command.add("-y");
        
        // Output file
        command.add(outputPath);
        
        return command;
    }

    /**
     * Build FFmpeg command for HLS generation
     */
    private List<String> buildHLSCommand(StreamSource streamSource, List<StreamTrack> selectedTracks,
                                       HLSProfile hlsProfile, String outputDir) {
        List<String> command = new ArrayList<>();
        
        command.add(ffmpegPath);
        command.add("-i");
        command.add(streamSource.getSourceUrl());
        
        // Video settings for HLS
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("fast");
        command.add("-crf");
        command.add("23");
        
        // Audio settings for HLS (always transcode to AAC for WebOS)
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-ac");
        command.add("2");
        
        // HLS specific settings
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add(String.valueOf(hlsSegmentDuration));
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_segment_filename");
        command.add(outputDir + "/segment_%03d.ts");
        
        // Progress reporting
        command.add("-progress");
        command.add("pipe:1");
        
        // Overwrite output
        command.add("-y");
        
        // Output playlist
        command.add(outputDir + "/playlist.m3u8");
        
        return command;
    }

    /**
     * Start transcoding process in background thread
     */
    private void startTranscodingProcess(TranscodingJob job, List<String> command) {
        Thread transcodingThread = new Thread(() -> {
            try {
                executeTranscodingJob(job, command);
            } catch (Exception e) {
                logger.error("Transcoding job failed: {}", job.getId(), e);
                job.fail(e.getMessage());
                jobRepository.save(job);
            }
        });
        
        transcodingThread.setName("Transcoding-" + job.getId());
        transcodingThread.start();
    }

    /**
     * Execute transcoding job with progress monitoring
     */
    private void executeTranscodingJob(TranscodingJob job, List<String> command) throws IOException, InterruptedException {
        logger.info("Executing transcoding job: {} with command: {}", job.getId(), String.join(" ", command));
        
        // Ensure output directory exists
        Path outputPath = Paths.get(job.getOutputFile()).getParent();
        if (outputPath != null) {
            Files.createDirectories(outputPath);
        }
        
        // Start job
        job.start();
        jobRepository.save(job);
        
        // Execute FFmpeg process
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        StringBuilder logOutput = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logOutput.append(line).append("\n");
                
                // Parse progress information
                parseProgressLine(job, line);
                
                // Update job progress every 100 lines to avoid too frequent DB updates
                if (logOutput.length() % 10000 == 0) {
                    job.setFfmpegLog(logOutput.toString());
                    jobRepository.save(job);
                }
            }
        }
        
        // Wait for process completion
        boolean finished = process.waitFor(transcodingTimeoutSeconds, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Transcoding timed out after " + transcodingTimeoutSeconds + " seconds");
        }
        
        // Update job with final results
        job.setFfmpegLog(logOutput.toString());
        
        if (process.exitValue() == 0) {
            // Success
            job.complete();
            
            // Calculate output file size
            File outputFile = new File(job.getOutputFile());
            if (outputFile.exists()) {
                job.setOutputSizeBytes(outputFile.length());
            }
            
            logger.info("Transcoding job completed successfully: {}", job.getId());
        } else {
            // Failure
            job.fail("FFmpeg process failed with exit code: " + process.exitValue());
            logger.error("Transcoding job failed: {} with exit code: {}", job.getId(), process.exitValue());
        }
        
        jobRepository.save(job);
    }

    /**
     * Parse FFmpeg progress output
     */
    private void parseProgressLine(TranscodingJob job, String line) {
        try {
            // Parse frame number
            Matcher frameMatcher = FRAME_PATTERN.matcher(line);
            if (frameMatcher.find()) {
                long currentFrame = Long.parseLong(frameMatcher.group(1));
                job.setCurrentFrame(currentFrame);
                
                // Calculate progress percentage if total frames is known
                if (job.getTotalFrames() != null && job.getTotalFrames() > 0) {
                    int progress = (int) ((currentFrame * 100) / job.getTotalFrames());
                    job.setProgressPercent(Math.min(progress, 99)); // Never show 100% until complete
                }
            }
            
            // Parse processing speed
            Matcher speedMatcher = SPEED_PATTERN.matcher(line);
            if (speedMatcher.find()) {
                job.setProcessingSpeed(speedMatcher.group(1) + "x");
            }
            
            // Parse time progress
            Matcher timeMatcher = TIME_PATTERN.matcher(line);
            if (timeMatcher.find()) {
                int hours = Integer.parseInt(timeMatcher.group(1));
                int minutes = Integer.parseInt(timeMatcher.group(2));
                int seconds = Integer.parseInt(timeMatcher.group(3));
                
                int totalSeconds = hours * 3600 + minutes * 60 + seconds;
                
                // Estimate completion time if we know the total duration
                if (job.getStreamSource().getDurationSeconds() != null) {
                    int totalDuration = job.getStreamSource().getDurationSeconds();
                    if (totalDuration > 0 && totalSeconds > 0) {
                        int progress = (int) ((totalSeconds * 100) / totalDuration);
                        job.setProgressPercent(Math.min(progress, 99));
                        
                        // Estimate completion time based on current speed
                        if (job.getProcessingSpeed() != null) {
                            try {
                                double speed = Double.parseDouble(job.getProcessingSpeed().replace("x", ""));
                                if (speed > 0) {
                                    int remainingSeconds = (int) ((totalDuration - totalSeconds) / speed);
                                    job.setEstimatedCompletionAt(LocalDateTime.now().plusSeconds(remainingSeconds));
                                }
                            } catch (NumberFormatException e) {
                                // Ignore parsing errors
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Ignore parsing errors, continue processing
            logger.debug("Failed to parse progress line: {}", line, e);
        }
    }

    /**
     * Generate output path for transcoded file
     */
    private String generateOutputPath(StreamSource streamSource, TranscodingProfile profile) {
        String filename = String.format("stream_%d_%s.%s", 
                                       streamSource.getId(), 
                                       profile.getName().toLowerCase(),
                                       profile.getOutputFormat());
        
        return Paths.get(outputDirectory, "transcoded", filename).toString();
    }

    /**
     * Generate HLS output directory path
     */
    private String generateHLSOutputPath(StreamSource streamSource, HLSProfile profile) {
        String dirName = String.format("stream_%d_%s_hls", 
                                     streamSource.getId(), 
                                     profile.getName().toLowerCase());
        
        return Paths.get(outputDirectory, "hls", dirName).toString();
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
     * Check if FFmpeg is available
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            logger.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    // Transcoding profile classes
    public static class TranscodingProfile {
        private final String name;
        private final String videoCodec;
        private final String videoPreset;
        private final int videoCrf;
        private final int maxWidth;
        private final String audioCodec;
        private final int audioBitrate;
        private final int maxAudioChannels;
        private final String outputFormat;
        private final boolean forceReencode;

        public TranscodingProfile(String name, String videoCodec, String videoPreset, int videoCrf,
                                int maxWidth, String audioCodec, int audioBitrate, int maxAudioChannels,
                                String outputFormat, boolean forceReencode) {
            this.name = name;
            this.videoCodec = videoCodec;
            this.videoPreset = videoPreset;
            this.videoCrf = videoCrf;
            this.maxWidth = maxWidth;
            this.audioCodec = audioCodec;
            this.audioBitrate = audioBitrate;
            this.maxAudioChannels = maxAudioChannels;
            this.outputFormat = outputFormat;
            this.forceReencode = forceReencode;
        }

        // Getters
        public String getName() { return name; }
        public String getVideoCodec() { return videoCodec; }
        public String getVideoPreset() { return videoPreset; }
        public int getVideoCrf() { return videoCrf; }
        public int getMaxWidth() { return maxWidth; }
        public String getAudioCodec() { return audioCodec; }
        public int getAudioBitrate() { return audioBitrate; }
        public int getMaxAudioChannels() { return maxAudioChannels; }
        public String getOutputFormat() { return outputFormat; }
        public boolean isForceReencode() { return forceReencode; }

        // Predefined profiles
        public static final TranscodingProfile WEBOS_COMPATIBLE = new TranscodingProfile(
                "WebOS_Compatible", "libx264", "fast", 23, 1920, "aac", 128, 2, "mp4", false);
        
        public static final TranscodingProfile WEBOS_OPTIMIZED = new TranscodingProfile(
                "WebOS_Optimized", "libx264", "medium", 25, 1280, "aac", 96, 2, "mp4", true);
    }

    public static class HLSProfile {
        private final String name;
        private final int segmentDuration;
        private final String videoCodec;
        private final String audioCodec;

        public HLSProfile(String name, int segmentDuration, String videoCodec, String audioCodec) {
            this.name = name;
            this.segmentDuration = segmentDuration;
            this.videoCodec = videoCodec;
            this.audioCodec = audioCodec;
        }

        public String getName() { return name; }
        public int getSegmentDuration() { return segmentDuration; }
        public String getVideoCodec() { return videoCodec; }
        public String getAudioCodec() { return audioCodec; }

        // Predefined HLS profiles
        public static final HLSProfile WEBOS_HLS = new HLSProfile("WebOS_HLS", 6, "libx264", "aac");
    }
}