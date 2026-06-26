-- Support soft-delete on messages
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;

-- When a message is deleted, clients should receive the update via Realtime
-- The content is kept server-side for moderation; clients show placeholder text

-- RLS: only the sender can soft-delete their own message
CREATE POLICY "sender can delete own message"
    ON messages FOR UPDATE
    TO authenticated
    USING (auth.uid()::text = sender_id)
    WITH CHECK (auth.uid()::text = sender_id);
