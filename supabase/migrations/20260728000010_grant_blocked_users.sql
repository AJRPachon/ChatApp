-- ============================================================
-- Fix: blocked_users has RLS policies but was never included in
-- the least-privilege GRANT migration (20260625_minimal_grants.sql).
-- Without a table-level GRANT, Postgres denies access before RLS
-- is even evaluated, surfacing as
-- "permission denied for table blocked_users".
--
-- This table is read via InvitationRemoteSource.getRelationshipInvitations
-- (blocking-check as part of contact-relationship resolution), which
-- runs whenever a ContactBubble renders (CheckContactRelationship).
--
-- SELECT: as blocker or as the blocked user (RLS scopes rows)
-- INSERT: user creates a block as blocker_id = auth.uid()
-- DELETE: user removes their own block (unblock)
-- UPDATE: not needed — blocks are immutable, recreate to change reason
-- ============================================================

REVOKE ALL ON public.blocked_users FROM authenticated;
GRANT SELECT, INSERT, DELETE ON public.blocked_users TO authenticated;
