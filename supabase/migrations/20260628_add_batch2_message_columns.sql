-- Add batch2 message feature columns to messages table.
-- These support soft-delete, edit, self-destruct, E2EE, and file/video attachments.

ALTER TABLE messages
  ADD COLUMN IF NOT EXISTS is_deleted    BOOLEAN    NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS is_edited     BOOLEAN    NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS edited_at     TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS expires_at    TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS is_encrypted  BOOLEAN    NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS file_url      TEXT,
  ADD COLUMN IF NOT EXISTS file_name     TEXT,
  ADD COLUMN IF NOT EXISTS file_size     BIGINT,
  ADD COLUMN IF NOT EXISTS file_mime_type TEXT,
  ADD COLUMN IF NOT EXISTS video_url     TEXT;
