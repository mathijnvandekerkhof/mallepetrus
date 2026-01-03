-- Fix JSONB column type issue
ALTER TABLE security_events ALTER COLUMN details TYPE TEXT;