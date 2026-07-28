-- Fix infinite recursion in conversation_participants SELECT policy.
-- The participants_select policy checks conversation_participants inside itself,
-- causing PG error 42P17 whenever conversations or participants are queried.
--
-- Same fix pattern as invitations: SECURITY DEFINER helper bypasses RLS.

create or replace function public.is_conversation_participant(
    p_conversation_id uuid,
    p_user_id         uuid
)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
    select exists (
        select 1
        from public.conversation_participants
        where conversation_id = p_conversation_id
          and user_id         = p_user_id
    );
$$;

-- Replace the recursive participants SELECT policy.
drop policy if exists "participants_select" on public.conversation_participants;

create policy "participants_select" on public.conversation_participants
    for select to authenticated
    using (
        public.is_conversation_participant(conversation_id, (select auth.uid()))
    );

-- The conversations_select policy also checks conversation_participants via EXISTS —
-- rewrite it to use the helper function so it too avoids recursion.
drop policy if exists "conversations_select" on public.conversations;

create policy "conversations_select" on public.conversations
    for select to authenticated
    using (
        public.is_conversation_participant(id, (select auth.uid()))
    );
