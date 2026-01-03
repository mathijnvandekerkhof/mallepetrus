-- Zero Trust Architecture Tables

-- Device fingerprints and tracking
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fingerprint_hash VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(255),
    user_agent TEXT,
    ip_address INET,
    screen_resolution VARCHAR(50),
    timezone VARCHAR(100),
    language VARCHAR(10),
    platform VARCHAR(100),
    is_trusted BOOLEAN DEFAULT FALSE,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    access_count INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Security events and risk assessments
CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint_id BIGINT REFERENCES device_fingerprints(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAILURE, MFA_FAILURE, SUSPICIOUS_LOCATION, etc.
    risk_score INTEGER NOT NULL DEFAULT 0, -- 0-100 scale
    ip_address INET,
    location_country VARCHAR(2),
    location_city VARCHAR(100),
    user_agent TEXT,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Risk assessment rules and thresholds
CREATE TABLE risk_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    rule_type VARCHAR(50) NOT NULL, -- LOCATION, DEVICE, BEHAVIOR, TIME_BASED
    risk_weight INTEGER NOT NULL DEFAULT 10, -- How much this rule contributes to risk score
    threshold_value VARCHAR(255), -- Configurable threshold (JSON or simple value)
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User sessions with risk tracking
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint_id BIGINT REFERENCES device_fingerprints(id) ON DELETE SET NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    initial_risk_score INTEGER NOT NULL DEFAULT 0,
    current_risk_score INTEGER NOT NULL DEFAULT 0,
    ip_address INET,
    location_country VARCHAR(2),
    location_city VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    requires_step_up_auth BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trusted locations for users
CREATE TABLE trusted_locations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    location_name VARCHAR(255),
    country_code VARCHAR(2),
    city VARCHAR(100),
    ip_range_start INET,
    ip_range_end INET,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_device_fingerprints_user_id ON device_fingerprints(user_id);
CREATE INDEX idx_device_fingerprints_hash ON device_fingerprints(fingerprint_hash);
CREATE INDEX idx_device_fingerprints_last_seen ON device_fingerprints(last_seen_at);

CREATE INDEX idx_security_events_user_id ON security_events(user_id);
CREATE INDEX idx_security_events_type ON security_events(event_type);
CREATE INDEX idx_security_events_created_at ON security_events(created_at);
CREATE INDEX idx_security_events_risk_score ON security_events(risk_score);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_active ON user_sessions(is_active);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

CREATE INDEX idx_trusted_locations_user_id ON trusted_locations(user_id);

-- Insert default risk rules
INSERT INTO risk_rules (rule_name, rule_type, risk_weight, threshold_value, description) VALUES
('NEW_DEVICE', 'DEVICE', 25, '{}', 'New device detected for user'),
('UNKNOWN_LOCATION', 'LOCATION', 30, '{}', 'Login from unknown location'),
('SUSPICIOUS_IP', 'LOCATION', 40, '{}', 'Login from suspicious IP address'),
('MULTIPLE_FAILED_ATTEMPTS', 'BEHAVIOR', 35, '{"max_attempts": 3, "time_window": 300}', 'Multiple failed login attempts'),
('UNUSUAL_TIME', 'TIME_BASED', 15, '{"normal_hours": "06:00-23:00"}', 'Login outside normal hours'),
('RAPID_LOCATION_CHANGE', 'LOCATION', 45, '{"max_distance_km": 1000, "time_window": 3600}', 'Impossible travel detected'),
('MFA_BYPASS_ATTEMPT', 'BEHAVIOR', 50, '{}', 'Attempt to bypass MFA'),
('CONCURRENT_SESSIONS', 'BEHAVIOR', 20, '{"max_sessions": 3}', 'Too many concurrent sessions');