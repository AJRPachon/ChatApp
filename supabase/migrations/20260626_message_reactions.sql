CREATE TABLE IF NOT EXISTS message_reactions (
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    emoji TEXT NOT NULL CHECK (char_length(emoji) <= 8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, user_id, emoji)
);

ALTER TABLE message_reactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "users can view reactions in their conversations"
    ON message_reactions FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM conversation_members cm
            JOIN messages m ON m.conversation_id = cm.conversation_id
            WHERE m.id = message_reactions.message_id AND cm.user_id = auth.uid()
        )
    );

CREATE POLICY "users can add their own reactions"
    ON message_reactions FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "users can remove their own reactions"
    ON message_reactions FOR DELETE TO authenticated
    USING (user_id = auth.uid());

CREATE INDEX IF NOT EXISTS idx_message_reactions_message_id ON message_reactions (message_id);
