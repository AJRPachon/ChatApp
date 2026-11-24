-- Group admins can change another participant's role (promote to admin / demote to member).
--
-- Found by the Maestro E2E suite (flows/09_groups/create_and_manage_group.yaml): the only UPDATE
-- policy on conversation_participants was "allow_self_promote_when_no_admin", so
-- GroupRemoteSource.updateMemberRole() always matched 0 rows under RLS and "Make admin" /
-- "Remove admin" silently did nothing (PostgREST returns success for a 0-row update).
--
-- USING: the caller must be an admin of that conversation (is_group_admin checks auth.uid()).
-- WITH CHECK: same, and only the two valid roles can be written.
create policy "admin_update_member_role"
    on public.conversation_participants
    for update
    using (is_group_admin(conversation_id))
    with check (is_group_admin(conversation_id) and role in ('admin', 'member'));
