-- Reply-to-status (WhatsApp-style story replies): adds a snapshot of the status being
-- replied to directly on the message row, taken at reply time — not a live reference. This
-- lets the chat bubble show the quoted status without a join, and lets it correctly show
-- "no longer available" once reply_to_status_expires_at passes, without depending on the
-- underlying user_status row still existing (it gets locally/eventually cleaned up anyway).
-- See StatusReplyContext (Kotlin) for the client-side mirror of this shape.

alter table public.messages
    add column if not exists reply_to_status_id text,
    add column if not exists reply_to_status_owner_id uuid,
    add column if not exists reply_to_status_text text,
    add column if not exists reply_to_status_image_url text,
    add column if not exists reply_to_status_video_url text,
    add column if not exists reply_to_status_background_color bigint,
    add column if not exists reply_to_status_expires_at timestamptz;
