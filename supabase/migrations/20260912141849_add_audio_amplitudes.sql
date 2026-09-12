-- Persist the recorded audio waveform (comma-separated, resampled amplitudes) so a received
-- voice message can render the sender's real waveform at rest, not just a synthetic one
-- generated from the audio URL's hash.

ALTER TABLE messages
  ADD COLUMN IF NOT EXISTS audio_amplitudes TEXT;
