-- TV Device Pairing Schema
-- V4: Add TV devices and pairing tokens for WebOS TV integration

-- TV Devices table - stores paired WebOS TV devices
CREATE TABLE tv_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(255) NOT NULL,
    mac_address VARCHAR(17) NOT NULL UNIQUE, -- MAC address format: XX:XX:XX:XX:XX:XX
    mac_address_hash VARCHAR(255) NOT NULL UNIQUE, -- Hashed MAC for security
    webos_version VARCHAR(50),
    model_name VARCHAR(255),
    manufacturer VARCHAR(100) DEFAULT 'LG',
    screen_resolution VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_seen_at TIMESTAMP,
    paired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Device Pairing Tokens table - temporary tokens for QR code pairing
CREATE TABLE device_pairing_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pairing_token VARCHAR(64) NOT NULL UNIQUE, -- Secure random token for QR code
    qr_code_data TEXT NOT NULL, -- JSON data embedded in QR code
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMP,
    tv_device_id BIGINT REFERENCES tv_devices(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_tv_devices_user_id ON tv_devices(user_id);
CREATE INDEX idx_tv_devices_mac_hash ON tv_devices(mac_address_hash);
CREATE INDEX idx_tv_devices_active ON tv_devices(is_active);
CREATE INDEX idx_pairing_tokens_user_id ON device_pairing_tokens(user_id);
CREATE INDEX idx_pairing_tokens_token ON device_pairing_tokens(pairing_token);
CREATE INDEX idx_pairing_tokens_expires ON device_pairing_tokens(expires_at);
CREATE INDEX idx_pairing_tokens_used ON device_pairing_tokens(used);

-- Add constraints
ALTER TABLE tv_devices ADD CONSTRAINT chk_mac_address_format 
    CHECK (mac_address ~ '^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$');

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tv_devices_updated_at 
    BEFORE UPDATE ON tv_devices 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();