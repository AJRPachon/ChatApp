-- Tabla para guardar tokens FCM por usuario
CREATE TABLE IF NOT EXISTS fcm_tokens (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  token   TEXT NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE fcm_tokens ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage own token" ON fcm_tokens
  FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON fcm_tokens TO authenticated;

-- Nota: el trigger se gestiona como Database Webhook en el dashboard de Supabase
-- (Database → Webhooks → on_new_message → INSERT en messages → send-fcm-notification)
