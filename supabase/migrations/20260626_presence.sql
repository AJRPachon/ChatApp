-- Add presence columns to profiles table
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS last_seen TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS show_online_status BOOLEAN NOT NULL DEFAULT true;

-- Index to efficiently query online users (last seen within 2 minutes)
CREATE INDEX IF NOT EXISTS idx_profiles_last_seen ON profiles (last_seen DESC)
    WHERE last_seen IS NOT NULL;

-- RLS: anyone can read last_seen and show_online_status of any profile
-- (already covered by existing profiles SELECT policy)
-- Only the owner can update their own presence fields
CREATE POLICY "users can update own presence"
    ON profiles FOR UPDATE
    TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);
