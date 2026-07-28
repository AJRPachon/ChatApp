-- Fix a copy-paste bug in messages_insert / participants_insert / participants_delete:
-- their inline EXISTS subqueries compared conversation_participants.conversation_id to
-- itself (cp.conversation_id = cp.conversation_id), which is always true and is not
-- scoped to the row being inserted/deleted. That made the WITH CHECK/USING clauses
-- overly permissive (any authenticated user who is a participant of ANY conversation
-- satisfied the check for ANY conversation_id) — a real BOLA-style security hole.
--
-- These bare EXISTS subqueries also queried conversation_participants directly under
-- the invoking role's own privileges rather than through the SECURITY DEFINER helpers
-- already used to fix the earlier participants_select/conversations_select recursion
-- (see 20260628000004_fix_participants_rls_recursion.sql). Routing them through
-- is_conversation_participant()/is_group_admin() (both SECURITY DEFINER, owned by
-- postgres, bypassrls) removes any dependency on live grants for authenticated when
-- evaluating the nested subquery, which is the most likely explanation for the
-- "permission denied for table conversation_participants" error seen only through
-- PostgREST and not via a direct SQL simulation under SET ROLE authenticated.

drop policy if exists "messages_insert" on public.messages;

create policy "messages_insert" on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and public.is_conversation_participant(conversation_id, (select auth.uid()))
    );

drop policy if exists "participants_insert" on public.conversation_participants;

create policy "participants_insert" on public.conversation_participants
    for insert to authenticated
    with check (
        user_id = (select auth.uid())
        or public.is_group_admin(conversation_id)
    );

drop policy if exists "participants_delete" on public.conversation_participants;

create policy "participants_delete" on public.conversation_participants
    for delete to authenticated
    using (
        user_id = (select auth.uid())
        or public.is_group_admin(conversation_id)
    );
