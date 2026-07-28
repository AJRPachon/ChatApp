-- ============================================================
-- Minimal Postgres GRANTs — Principle of Least Privilege
--
-- Purpose: Restrict table-level privileges for the `authenticated`
-- role to only the operations that are actually exercised by RLS
-- policies. GRANTs are a coarse gate: they determine which SQL
-- verbs Postgres even considers before evaluating RLS. Removing
-- verbs that are never used (e.g. DELETE on messages, UPDATE on
-- call_signals) shrinks the attack surface even if RLS would have
-- blocked the row anyway.
--
-- When to update this file:
--   • A new feature needs a previously-revoked operation on a table
--     → Add a GRANT here AND add the corresponding RLS policy in
--       the same PR/migration.
--   • A table is dropped → remove its REVOKE/GRANT block.
--   • A new table is added → add a REVOKE ALL + targeted GRANT
--     block before merging the table's creation migration.
--
-- The `anon` role intentionally receives no table access; all
-- unauthenticated paths must go through Supabase Auth first.
-- ============================================================

-- ── profiles ─────────────────────────────────────────────────
-- SELECT: all authenticated users (directory lookup)
-- INSERT: own row on sign-up
-- UPDATE: own row (display name, avatar, etc.)
-- DELETE: not supported — account deletion goes through Auth
REVOKE ALL ON public.profiles FROM authenticated;
GRANT SELECT, INSERT, UPDATE ON public.profiles TO authenticated;

-- ── conversations ─────────────────────────────────────────────
-- SELECT: participants only (RLS enforces membership check)
-- INSERT: creator sets created_by = auth.uid()
-- UPDATE: admin or creator can rename/update metadata
-- DELETE: not supported
REVOKE ALL ON public.conversations FROM authenticated;
GRANT SELECT, INSERT, UPDATE ON public.conversations TO authenticated;

-- ── conversation_participants ──────────────────────────────────
-- SELECT: members of the same conversation
-- INSERT: self-join or admin adds another user
-- DELETE: self-leave or admin removes a member
-- UPDATE: not needed — role changes done via delete+insert pattern
REVOKE ALL ON public.conversation_participants FROM authenticated;
GRANT SELECT, INSERT, DELETE ON public.conversation_participants TO authenticated;

-- ── messages ──────────────────────────────────────────────────
-- SELECT: conversation participants
-- INSERT: participant sending as themselves
-- UPDATE: sender can edit their own message (RLS policy exists)
-- DELETE: not supported — messages are retained for conversation history
REVOKE ALL ON public.messages FROM authenticated;
GRANT SELECT, INSERT, UPDATE ON public.messages TO authenticated;

-- ── calls ─────────────────────────────────────────────────────
-- SELECT: caller, callee, or conversation participant
-- INSERT: caller initiates (caller_id = auth.uid())
-- UPDATE: caller/callee update status (ringing → active → ended)
-- DELETE: not supported — call records are audit history
REVOKE ALL ON public.calls FROM authenticated;
GRANT SELECT, INSERT, UPDATE ON public.calls TO authenticated;

-- ── call_signals ──────────────────────────────────────────────
-- SELECT: caller or callee of the associated call
-- INSERT: call participant sends a signal (offer/answer/ICE)
-- UPDATE: not needed — signals are immutable once sent
-- DELETE: not needed — signals are short-lived but retained per call
REVOKE ALL ON public.call_signals FROM authenticated;
GRANT SELECT, INSERT ON public.call_signals TO authenticated;

-- ── invitations ───────────────────────────────────────────────
-- SELECT: sender or receiver
-- INSERT: sender (cannot invite self, no duplicate pending)
-- UPDATE: receiver accepts or rejects (status field only)
-- DELETE: not supported — invitation history is retained
REVOKE ALL ON public.invitations FROM authenticated;
GRANT SELECT, INSERT, UPDATE ON public.invitations TO authenticated;

-- ── Sequences ─────────────────────────────────────────────────
-- Required if any table uses a serial/bigserial primary key or
-- nextval()-based default. Safe to include even if all PKs are UUIDs.
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO authenticated;

-- ── anon role — no table access ───────────────────────────────
-- All unauthenticated clients must authenticate via Supabase Auth
-- before any table data is accessible. Explicit revoke ensures
-- that a future GRANT ALL IN SCHEMA ... does not accidentally
-- re-open access to anon.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon;
