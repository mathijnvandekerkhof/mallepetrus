package nl.mallepetrus.jiptv.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "stream_tracks")
@EntityListeners(AuditingEntityListener.class)
public class StreamTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_source_id", nullable = false)
    private StreamSource streamSource;

    @NotNull
    @Column(name = "track_index", nullable = false)
    private Integer trackIndex;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", nullable = false)
    private TrackType trackType;

    @Column(name = "codec_name")
    private String codecName;

    @Column(name = "codec_long_name")
    private String codecLongName;

    @Column(length = 10)
    private String language;

    private String title;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "is_forced", nullable = false)
    private boolean isForced = false;

    // Video specific fields
    private Integer width;
    private Integer height;
    @Column(name = "frame_rate")
    private String frameRate;
    @Column(name = "pixel_format")
    private String pixelFormat;
    @Column(name = "color_space")
    private String colorSpace;
    @Column(name = "hdr_metadata", columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String hdrMetadata; // JSON string for HDR metadata

    // Audio specific fields
    private Integer channels;
    @Column(name = "channel_layout")
    private String channelLayout;
    @Column(name = "sample_rate")
    private Integer sampleRate;
    @Column(name = "bit_depth")
    private Integer bitDepth;

    // Common fields
    private Long bitrate;
    @Column(name = "duration_seconds", precision = 10, scale = 3)
    private BigDecimal durationSeconds;
    @Column(name = "webos_compatible", nullable = false)
    private boolean webosCompatible = false;
    @Column(name = "transcoding_required", nullable = false)
    private boolean transcodingRequired = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public StreamTrack() {}

    public StreamTrack(StreamSource streamSource, Integer trackIndex, TrackType trackType) {
        this.streamSource = streamSource;
        this.trackIndex = trackIndex;
        this.trackType = trackType;
    }

    // Enums
    public enum TrackType {
        VIDEO,
        AUDIO,
        SUBTITLE
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

    public Integer getTrackIndex() {
        return trackIndex;
    }

    public void setTrackIndex(Integer trackIndex) {
        this.trackIndex = trackIndex;
    }

    public TrackType getTrackType() {
        return trackType;
    }

    public void setTrackType(TrackType trackType) {
        this.trackType = trackType;
    }

    public String getCodecName() {
        return codecName;
    }

    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    public String getCodecLongName() {
        return codecLongName;
    }

    public void setCodecLongName(String codecLongName) {
        this.codecLongName = codecLongName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isForced() {
        return isForced;
    }

    public void setForced(boolean isForced) {
        this.isForced = isForced;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(String frameRate) {
        this.frameRate = frameRate;
    }

    public String getPixelFormat() {
        return pixelFormat;
    }

    public void setPixelFormat(String pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    public String getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(String colorSpace) {
        this.colorSpace = colorSpace;
    }

    public String getHdrMetadata() {
        return hdrMetadata;
    }

    public void setHdrMetadata(String hdrMetadata) {
        this.hdrMetadata = hdrMetadata;
    }

    public Integer getChannels() {
        return channels;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    public String getChannelLayout() {
        return channelLayout;
    }

    public void setChannelLayout(String channelLayout) {
        this.channelLayout = channelLayout;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Integer getBitDepth() {
        return bitDepth;
    }

    public void setBitDepth(Integer bitDepth) {
        this.bitDepth = bitDepth;
    }

    public Long getBitrate() {
        return bitrate;
    }

    public void setBitrate(Long bitrate) {
        this.bitrate = bitrate;
    }

    public BigDecimal getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(BigDecimal durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isWebosCompatible() {
        return webosCompatible;
    }

    public void setWebosCompatible(boolean webosCompatible) {
        this.webosCompatible = webosCompatible;
    }

    public boolean isTranscodingRequired() {
        return transcodingRequired;
    }

    public void setTranscodingRequired(boolean transcodingRequired) {
        this.transcodingRequired = transcodingRequired;
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
    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        
        if (title != null && !title.trim().isEmpty()) {
            name.append(title);
        } else {
            name.append(trackType.toString().toLowerCase());
            if (trackIndex != null) {
                name.append(" ").append(trackIndex);
            }
        }
        
        if (language != null && !language.trim().isEmpty()) {
            name.append(" (").append(language.toUpperCase()).append(")");
        }
        
        return name.toString();
    }

    public String getResolution() {
        if (width != null && height != null) {
            return width + "x" + height;
        }
        return null;
    }

    public String getFormattedBitrate() {
        if (bitrate == null) return "Unknown";
        
        if (bitrate >= 1_000_000) {
            return String.format("%.1f Mbps", bitrate / 1_000_000.0);
        } else if (bitrate >= 1_000) {
            return String.format("%.0f kbps", bitrate / 1_000.0);
        } else {
            return bitrate + " bps";
        }
    }

    public boolean isHdr() {
        return hdrMetadata != null && !hdrMetadata.trim().isEmpty();
    }
}