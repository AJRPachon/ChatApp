-- Fix infinite recursion in invitations_insert RLS policy.
-- The NOT EXISTS subquery on public.invitations inside the INSERT policy
-- causes PostgreSQL to re-evaluate the INSERT policy for each row it reads
-- from the subquery, creating an infinite recursion (PG error 42P17).
--
-- Solution: wrap the duplicate-check in a SECURITY DEFINER function that
-- executes with the function owner's privileges, bypassing RLS entirely.

create or replace function public.has_pending_invitation(
    p_sender  uuid,
    p_receiver uuid
)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
    select exists (
        select 1
        from public.invitations
        where sender_id  = p_sender
          and receiver_id = p_receiver
          and status      = 'pending'
    );
$$;

-- Replace the recursive INSERT policy with one that calls the helper function.
drop policy if exists "invitations_insert" on public.invitations;

create policy "invitations_insert" on public.invitations
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and sender_id <> receiver_id
        and not public.has_pending_invitation((select auth.uid()), receiver_id)
    );
