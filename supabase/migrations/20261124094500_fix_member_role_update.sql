-- Make "Make admin" / "Remove admin" actually work (GroupRemoteSource.updateMemberRole).
--
-- Found by the Maestro E2E suite (flows/09_groups/create_and_manage_group.yaml) on a real
-- device: the PATCH returned 500 "infinite recursion detected in policy for relation
-- conversation_participants" (42P17), and once that is fixed it fails with 42501 because
-- 20260625070401_minimal_grants.sql never granted UPDATE on this table ("role changes done
-- via delete+insert"), while the client has always changed roles with an UPDATE.
--
-- 1. The UPDATE policy "allow_self_promote_when_no_admin" (created outside the migrations)
--    ran an inline EXISTS over conversation_participants inside a policy of that same table,
--    which re-enters its RLS. Move the lookup into a SECURITY DEFINER function, like
--    is_group_admin() already does.
create or replace function public.group_has_admin(conv_id uuid)
    returns boolean
    language sql
    stable
    security definer
    set search_path = public
as $$
    select exists (
        select 1
        from public.conversation_participants
        where conversation_id = conv_id
          and role = 'admin'
    );
$$;

revoke all on function public.group_has_admin(uuid) from public, anon;
grant execute on function public.group_has_admin(uuid) to authenticated;

alter policy "allow_self_promote_when_no_admin"
    on public.conversation_participants
    using ((user_id = auth.uid()) and not public.group_has_admin(conversation_id));

-- 2. Column-level grant: authenticated users may update the role column only (who can update
--    which row is still decided by the UPDATE policies: admin_update_member_role and
--    allow_self_promote_when_no_admin). conversation_id / user_id / joined_at stay read-only.
grant update (role) on public.conversation_participants to authenticated;
