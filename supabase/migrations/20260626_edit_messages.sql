-- Support editing messages (soft edit — original is overwritten)
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_edited BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS edited_at TIMESTAMPTZ;

-- RLS: only the sender can edit their own message
CREATE POLICY "sender can edit own message"
    ON messages FOR UPDATE
    TO authenticated
    USING (auth.uid()::text = sender_id)
    WITH CHECK (auth.uid()::text = sender_id);
