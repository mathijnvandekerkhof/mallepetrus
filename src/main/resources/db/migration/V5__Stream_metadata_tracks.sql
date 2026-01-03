-- Stream Metadata and Tracks Schema
-- V5: Add stream analysis, track management, and user preferences

-- Stream Sources table - IPTV stream sources and files
CREATE TABLE stream_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(50) NOT NULL, -- 'FILE', 'URL', 'IPTV_CHANNEL'
    source_url TEXT NOT NULL, -- File path or stream URL
    file_size BIGINT, -- For files, size in bytes
    file_hash VARCHAR(64), -- SHA-256 hash for files
    content_type VARCHAR(100), -- MIME type
    duration_seconds INTEGER, -- Duration in seconds
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    analyzed_at TIMESTAMP, -- When FFprobe analysis was done
    analysis_version VARCHAR(20) DEFAULT '1.0' -- Track analysis version
);

-- Stream Tracks table - Video/Audio/Subtitle tracks from FFprobe
CREATE TABLE stream_tracks (
    id BIGSERIAL PRIMARY KEY,
    stream_source_id BIGINT NOT NULL REFERENCES stream_sources(id) ON DELETE CASCADE,
    track_index INTEGER NOT NULL, -- Track index from FFprobe
    track_type VARCHAR(20) NOT NULL, -- 'VIDEO', 'AUDIO', 'SUBTITLE'
    codec_name VARCHAR(50), -- H.264, AAC, SRT, etc.
    codec_long_name VARCHAR(255),
    language VARCHAR(10), -- ISO 639-1/639-2 language code
    title VARCHAR(255), -- Track title/name
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_forced BOOLEAN NOT NULL DEFAULT false,
    
    -- Video specific fields
    width INTEGER,
    height INTEGER,
    frame_rate VARCHAR(20), -- "25/1", "30000/1001"
    pixel_format VARCHAR(50),
    color_space VARCHAR(50),
    hdr_metadata JSONB, -- HDR10, Dolby Vision metadata
    
    -- Audio specific fields
    channels INTEGER, -- Number of audio channels
    channel_layout VARCHAR(50), -- "5.1", "7.1", "stereo"
    sample_rate INTEGER, -- Sample rate in Hz
    bit_depth INTEGER, -- Bit depth (16, 24, 32)
    
    -- Common fields
    bitrate BIGINT, -- Bitrate in bits per second
    duration_seconds DECIMAL(10,3), -- Track duration
    webos_compatible BOOLEAN NOT NULL DEFAULT false, -- WebOS TV compatibility
    transcoding_required BOOLEAN NOT NULL DEFAULT false, -- Needs transcoding
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Stream Preferences table - User's preferred tracks per stream
CREATE TABLE user_stream_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stream_source_id BIGINT NOT NULL REFERENCES stream_sources(id) ON DELETE CASCADE,
    preferred_video_track_id BIGINT REFERENCES stream_tracks(id) ON DELETE SET NULL,
    preferred_audio_track_id BIGINT REFERENCES stream_tracks(id) ON DELETE SET NULL,
    preferred_subtitle_track_id BIGINT REFERENCES stream_tracks(id) ON DELETE SET NULL,
    subtitle_enabled BOOLEAN NOT NULL DEFAULT false,
    preferred_quality VARCHAR(20) DEFAULT 'AUTO', -- 'AUTO', '1080p', '720p', '480p'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, stream_source_id)
);

-- Stream Sessions table - Active streaming sessions
CREATE TABLE stream_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tv_device_id BIGINT NOT NULL REFERENCES tv_devices(id) ON DELETE CASCADE,
    stream_source_id BIGINT NOT NULL REFERENCES stream_sources(id) ON DELETE CASCADE,
    session_token VARCHAR(128) NOT NULL UNIQUE, -- Unique session identifier
    
    -- Selected tracks for this session
    selected_video_track_id BIGINT REFERENCES stream_tracks(id) ON DELETE SET NULL,
    selected_audio_track_id BIGINT REFERENCES stream_tracks(id) ON DELETE SET NULL,
    selected_subtitle_track_id BIGINT REFERENCES stream_tracks(id) ON DELETE SET NULL,
    
    -- Streaming details
    stream_url TEXT, -- Generated HLS/DASH URL
    quality VARCHAR(20) DEFAULT 'AUTO',
    transcoding_profile VARCHAR(50), -- Transcoding settings used
    
    -- Session state
    playback_position_seconds DECIMAL(10,3) DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transcoding Jobs table - FFmpeg transcoding job tracking
CREATE TABLE transcoding_jobs (
    id BIGSERIAL PRIMARY KEY,
    stream_source_id BIGINT NOT NULL REFERENCES stream_sources(id) ON DELETE CASCADE,
    job_type VARCHAR(50) NOT NULL, -- 'ANALYSIS', 'TRANSCODE', 'THUMBNAIL'
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'
    
    -- Job parameters
    input_file TEXT NOT NULL,
    output_file TEXT,
    ffmpeg_command TEXT, -- Full FFmpeg command
    transcoding_profile VARCHAR(50),
    target_tracks JSONB, -- Selected tracks for transcoding
    
    -- Progress tracking
    progress_percent INTEGER DEFAULT 0,
    current_frame BIGINT DEFAULT 0,
    total_frames BIGINT,
    processing_speed VARCHAR(20), -- "2.5x", "1.2x"
    
    -- Timing
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    estimated_completion_at TIMESTAMP,
    
    -- Results
    output_size_bytes BIGINT,
    error_message TEXT,
    ffmpeg_log TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_stream_sources_type ON stream_sources(source_type);
CREATE INDEX idx_stream_sources_active ON stream_sources(is_active);
CREATE INDEX idx_stream_sources_hash ON stream_sources(file_hash);

CREATE INDEX idx_stream_tracks_source ON stream_tracks(stream_source_id);
CREATE INDEX idx_stream_tracks_type ON stream_tracks(track_type);
CREATE INDEX idx_stream_tracks_language ON stream_tracks(language);
CREATE INDEX idx_stream_tracks_webos ON stream_tracks(webos_compatible);

CREATE INDEX idx_user_preferences_user ON user_stream_preferences(user_id);
CREATE INDEX idx_user_preferences_stream ON user_stream_preferences(stream_source_id);

CREATE INDEX idx_stream_sessions_user ON stream_sessions(user_id);
CREATE INDEX idx_stream_sessions_device ON stream_sessions(tv_device_id);
CREATE INDEX idx_stream_sessions_active ON stream_sessions(is_active);
CREATE INDEX idx_stream_sessions_token ON stream_sessions(session_token);

CREATE INDEX idx_transcoding_jobs_source ON transcoding_jobs(stream_source_id);
CREATE INDEX idx_transcoding_jobs_status ON transcoding_jobs(status);
CREATE INDEX idx_transcoding_jobs_type ON transcoding_jobs(job_type);

-- Add constraints
ALTER TABLE stream_tracks ADD CONSTRAINT chk_track_type 
    CHECK (track_type IN ('VIDEO', 'AUDIO', 'SUBTITLE'));

ALTER TABLE stream_sources ADD CONSTRAINT chk_source_type 
    CHECK (source_type IN ('FILE', 'URL', 'IPTV_CHANNEL'));

ALTER TABLE transcoding_jobs ADD CONSTRAINT chk_job_status 
    CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE transcoding_jobs ADD CONSTRAINT chk_job_type 
    CHECK (job_type IN ('ANALYSIS', 'TRANSCODE', 'THUMBNAIL', 'SEGMENT'));

-- Add trigger to update updated_at timestamp
CREATE TRIGGER update_stream_sources_updated_at 
    BEFORE UPDATE ON stream_sources 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stream_tracks_updated_at 
    BEFORE UPDATE ON stream_tracks 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_stream_preferences_updated_at 
    BEFORE UPDATE ON user_stream_preferences 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stream_sessions_updated_at 
    BEFORE UPDATE ON stream_sessions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transcoding_jobs_updated_at 
    BEFORE UPDATE ON transcoding_jobs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();