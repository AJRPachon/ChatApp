-- Persist the recorded audio duration so conversation previews can show it
-- (e.g. "Te envió un audio (0:07)") without downloading the audio file.

ALTER TABLE messages
  ADD COLUMN IF NOT EXISTS audio_duration_ms BIGINT;
