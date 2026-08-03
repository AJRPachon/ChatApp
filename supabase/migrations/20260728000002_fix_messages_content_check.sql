-- Fix messages_content_check to allow empty content for file/video messages.
-- The constraint was never updated when file_url/video_url were added
-- alongside image_url/audio_url/etc, so every file or video message
-- (which sends content = "") was rejected by Postgres with
-- "new row for relation messages violates check constraint messages_content_check".

ALTER TABLE public.messages
    DROP CONSTRAINT IF EXISTS messages_content_check;

ALTER TABLE public.messages
    ADD CONSTRAINT messages_content_check
    CHECK (
        content <> ''
        OR image_url IS NOT NULL
        OR audio_url IS NOT NULL
        OR call_type IS NOT NULL
        OR gif_url IS NOT NULL
        OR sticker_url IS NOT NULL
        OR file_url IS NOT NULL
        OR video_url IS NOT NULL
    );
