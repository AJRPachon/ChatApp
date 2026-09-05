-- ============================================================
-- Restore service_role's table privileges
--
-- 20260625070401_minimal_grants.sql (Principle of Least Privilege
-- hardening) only ever targeted the `authenticated` role
-- (`REVOKE ALL ... FROM authenticated` + a targeted re-GRANT per
-- table). It never touched `service_role`, but on this project
-- `service_role` turned out to have only SELECT on a handful of
-- tables (conversation_participants, conversations, fcm_tokens,
-- profiles) and NOTHING on every other table — no INSERT/UPDATE/
-- DELETE anywhere, no access at all to messages/calls/call_signals/
-- invitations/blocked_users/user_status.
--
-- This is NOT the intended Supabase security model: `service_role`
-- is the trusted backend role Edge Functions use precisely because
-- it bypasses RLS — but bypassing RLS and holding the underlying
-- Postgres table GRANT are two independent layers, and having the
-- former without the latter just means "backend code cannot touch
-- the table at all" rather than any actual security benefit (nothing
-- untrusted holds the service_role key; it's a Supabase secret used
-- only from Edge Functions).
--
-- Confirmed as a real, reproducible bug (not a hypothetical) while
-- deploying the delete-account Edge Function: every write it made
-- through the service-role client (`admin.from("messages").update()`,
-- `admin.from("conversations").update()`, etc.) failed with Postgres
-- "permission denied for table X" — a GRANT-level error, not an RLS
-- policy violation.
--
-- Restores full privileges for service_role on every table/sequence/
-- function in the public schema, and sets that as the default for
-- future tables too — matching Supabase's own documented model,
-- without touching (or weakening) any of the `authenticated`/`anon`
-- restrictions the minimal-grants hardening intentionally put in
-- place.
-- ============================================================

GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO service_role;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA public TO service_role;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO service_role;
