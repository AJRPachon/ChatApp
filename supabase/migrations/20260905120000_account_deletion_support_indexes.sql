-- ============================================================
-- Support indexes for the delete-account Edge Function
-- (supabase/functions/delete-account/index.ts).
--
-- Google Play requires an in-app account deletion path. The Edge
-- Function performs, per deleted user:
--   - a SELECT + UPDATE on messages filtered by sender_id (to scrub
--     content/attachments while preserving conversation history for
--     other participants — see the function's header comment for the
--     full anonymize-vs-delete reasoning)
--   - a DELETE on conversation_participants filtered by user_id
--   - a DELETE on invitations filtered by sender_id/receiver_id
--   - a DELETE on blocked_users filtered by blocker_id/blocked_id
--   - a DELETE on user_status filtered by user_id (already ON DELETE
--     CASCADE from profiles per 20260807092830_add_status_stories.sql;
--     this is a defense-in-depth explicit delete, not the only cleanup
--     path)
--
-- None of these columns had a supporting index (the base tables
-- predate tracked migrations in this repo, so we cannot rely on one
-- having been created out-of-band either), so each of these would be
-- a full table scan today. This is a one-time cost per account
-- deletion, but messages/conversation_participants/invitations grow
-- unbounded, so it is worth indexing up front rather than waiting for
-- it to show up as a slow query later.
--
-- Uses `if not exists` throughout — safe to run even if any of these
-- happen to already exist from an out-of-band dashboard change.
-- ============================================================

create index if not exists idx_messages_sender_id
    on public.messages (sender_id);

create index if not exists idx_conversation_participants_user_id
    on public.conversation_participants (user_id);

create index if not exists idx_invitations_sender_id
    on public.invitations (sender_id);

create index if not exists idx_invitations_receiver_id
    on public.invitations (receiver_id);

create index if not exists idx_blocked_users_blocker_id
    on public.blocked_users (blocker_id);

create index if not exists idx_blocked_users_blocked_id
    on public.blocked_users (blocked_id);
