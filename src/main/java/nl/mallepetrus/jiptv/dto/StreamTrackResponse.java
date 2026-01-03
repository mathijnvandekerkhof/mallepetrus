package nl.mallepetrus.jiptv.dto;

import nl.mallepetrus.jiptv.entity.StreamTrack;

import java.math.BigDecimal;

public class StreamTrackResponse {
    private Long id;
    private Integer trackIndex;
    private String trackType;
    private String codecName;
    private String codecLongName;
    private String language;
    private String title;
    private String displayName;
    private boolean isDefault;
    private boolean isForced;
    
    // Video specific
    private Integer width;
    private Integer height;
    private String resolution;
    private String frameRate;
    private String pixelFormat;
    private String colorSpace;
    private boolean isHdr;
    
    // Audio specific
    private Integer channels;
    private String channelLayout;
    private Integer sampleRate;
    private Integer bitDepth;
    
    // Common
    private Long bitrate;
    private String formattedBitrate;
    private BigDecimal durationSeconds;
    private boolean webosCompatible;
    private boolean transcodingRequired;

    public StreamTrackResponse() {}

    public StreamTrackResponse(StreamTrack track) {
        this.id = track.getId();
        this.trackIndex = track.getTrackIndex();
        this.trackType = track.getTrackType().name();
        this.codecName = track.getCodecName();
        this.codecLongName = track.getCodecLongName();
        this.language = track.getLanguage();
        this.title = track.getTitle();
        this.displayName = track.getDisplayName();
        this.isDefault = track.isDefault();
        this.isForced = track.isForced();
        
        // Video specific
        this.width = track.getWidth();
        this.height = track.getHeight();
        this.resolution = track.getResolution();
        this.frameRate = track.getFrameRate();
        this.pixelFormat = track.getPixelFormat();
        this.colorSpace = track.getColorSpace();
        this.isHdr = track.isHdr();
        
        // Audio specific
        this.channels = track.getChannels();
        this.channelLayout = track.getChannelLayout();
        this.sampleRate = track.getSampleRate();
        this.bitDepth = track.getBitDepth();
        
        // Common
        this.bitrate = track.getBitrate();
        this.formattedBitrate = track.getFormattedBitrate();
        this.durationSeconds = track.getDurationSeconds();
        this.webosCompatible = track.isWebosCompatible();
        this.transcodingRequired = track.isTranscodingRequired();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getTrackIndex() { return trackIndex; }
    public void setTrackIndex(Integer trackIndex) { this.trackIndex = trackIndex; }

    public String getTrackType() { return trackType; }
    public void setTrackType(String trackType) { this.trackType = trackType; }

    public String getCodecName() { return codecName; }
    public void setCodecName(String codecName) { this.codecName = codecName; }

    public String getCodecLongName() { return codecLongName; }
    public void setCodecLongName(String codecLongName) { this.codecLongName = codecLongName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public boolean isForced() { return isForced; }
    public void setForced(boolean isForced) { this.isForced = isForced; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getFrameRate() { return frameRate; }
    public void setFrameRate(String frameRate) { this.frameRate = frameRate; }

    public String getPixelFormat() { return pixelFormat; }
    public void setPixelFormat(String pixelFormat) { this.pixelFormat = pixelFormat; }

    public String getColorSpace() { return colorSpace; }
    public void setColorSpace(String colorSpace) { this.colorSpace = colorSpace; }

    public boolean isHdr() { return isHdr; }
    public void setHdr(boolean hdr) { isHdr = hdr; }

    public Integer getChannels() { return channels; }
    public void setChannels(Integer channels) { this.channels = channels; }

    public String getChannelLayout() { return channelLayout; }
    public void setChannelLayout(String channelLayout) { this.channelLayout = channelLayout; }

    public Integer getSampleRate() { return sampleRate; }
    public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }

    public Integer getBitDepth() { return bitDepth; }
    public void setBitDepth(Integer bitDepth) { this.bitDepth = bitDepth; }

    public Long getBitrate() { return bitrate; }
    public void setBitrate(Long bitrate) { this.bitrate = bitrate; }

    public String getFormattedBitrate() { return formattedBitrate; }
    public void setFormattedBitrate(String formattedBitrate) { this.formattedBitrate = formattedBitrate; }

    public BigDecimal getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(BigDecimal durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isWebosCompatible() { return webosCompatible; }
    public void setWebosCompatible(boolean webosCompatible) { this.webosCompatible = webosCompatible; }

    public boolean isTranscodingRequired() { return transcodingRequired; }
    public void setTranscodingRequired(boolean transcodingRequired) { this.transcodingRequired = transcodingRequired; }
}