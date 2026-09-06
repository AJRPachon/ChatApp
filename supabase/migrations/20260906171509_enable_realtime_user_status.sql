-- ============================================================
-- Enable Realtime broadcasts for public.user_status
--
-- Root cause of "I post a Status and the other person doesn't see it":
-- StatusRepositoryImpl.observeActiveStatuses() only ever synced once, on
-- StatusViewModel init (fixed client-side by subscribing
-- StatusRemoteSource.observeStatusChanges() — a Postgres Changes
-- realtime channel, same pattern as InvitationRemoteSource.
-- observeInvitations()). But the `user_status` table itself was never
-- added to the `supabase_realtime` publication when it was created
-- (20260807092830_add_status_stories.sql) — RLS alone does not enable
-- Realtime broadcasting for a table, the table must also be published.
-- Confirmed live: the client's Realtime channel connected fine but the
-- server rejected every postgres_changes subscription for this table
-- with "Unable to subscribe to changes ... Please check Realtime is
-- enabled for the given connect parameters ... table: user_status".
-- ============================================================

alter publication supabase_realtime add table public.user_status;
