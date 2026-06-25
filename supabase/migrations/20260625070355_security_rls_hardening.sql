-- ============================================================
-- RLS Hardening Migration
-- Ensures all tables have RLS enabled and policies are tight.
-- ============================================================

-- ── profiles ────────────────────────────────────────────────
alter table public.profiles enable row level security;

drop policy if exists "profiles_select" on public.profiles;
drop policy if exists "profiles_insert" on public.profiles;
drop policy if exists "profiles_update" on public.profiles;

create policy "profiles_select" on public.profiles
    for select to authenticated
    using (true);

create policy "profiles_insert" on public.profiles
    for insert to authenticated
    with check ((select auth.uid()) = id);

create policy "profiles_update" on public.profiles
    for update to authenticated
    using ((select auth.uid()) = id)
    with check ((select auth.uid()) = id);


-- ── conversations ────────────────────────────────────────────
alter table public.conversations enable row level security;

drop policy if exists "conversations_select" on public.conversations;
drop policy if exists "conversations_insert" on public.conversations;
drop policy if exists "conversations_update" on public.conversations;

create policy "conversations_select" on public.conversations
    for select to authenticated
    using (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = id
              and cp.user_id = (select auth.uid())
        )
    );

create policy "conversations_insert" on public.conversations
    for insert to authenticated
    with check ((select auth.uid()) = created_by);

create policy "conversations_update" on public.conversations
    for update to authenticated
    using (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = id
              and cp.user_id = (select auth.uid())
              and (cp.role = 'admin' or id = created_by)
        )
    )
    with check (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = id
              and cp.user_id = (select auth.uid())
              and (cp.role = 'admin' or id = created_by)
        )
    );


-- ── conversation_participants ────────────────────────────────
alter table public.conversation_participants enable row level security;

drop policy if exists "participants_select" on public.conversation_participants;
drop policy if exists "participants_insert" on public.conversation_participants;
drop policy if exists "participants_delete" on public.conversation_participants;

create policy "participants_select" on public.conversation_participants
    for select to authenticated
    using (
        exists (
            select 1 from public.conversation_participants cp2
            where cp2.conversation_id = conversation_id
              and cp2.user_id = (select auth.uid())
        )
    );

create policy "participants_insert" on public.conversation_participants
    for insert to authenticated
    with check (
        user_id = (select auth.uid())
        or
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = conversation_id
              and cp.user_id = (select auth.uid())
              and cp.role = 'admin'
        )
    );

create policy "participants_delete" on public.conversation_participants
    for delete to authenticated
    using (
        user_id = (select auth.uid())
        or
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = conversation_id
              and cp.user_id = (select auth.uid())
              and cp.role = 'admin'
        )
    );


-- ── messages ─────────────────────────────────────────────────
alter table public.messages enable row level security;

drop policy if exists "messages_select" on public.messages;
drop policy if exists "messages_insert" on public.messages;
drop policy if exists "messages_update" on public.messages;

create policy "messages_select" on public.messages
    for select to authenticated
    using (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = conversation_id
              and cp.user_id = (select auth.uid())
        )
    );

create policy "messages_insert" on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = conversation_id
              and cp.user_id = (select auth.uid())
        )
    );

create policy "messages_update" on public.messages
    for update to authenticated
    using (sender_id = (select auth.uid()))
    with check (sender_id = (select auth.uid()));


-- ── calls ────────────────────────────────────────────────────
alter table public.calls enable row level security;

drop policy if exists "calls_select" on public.calls;
drop policy if exists "calls_insert" on public.calls;
drop policy if exists "calls_update" on public.calls;

create policy "calls_select" on public.calls
    for select to authenticated
    using (
        caller_id = (select auth.uid())
        or callee_id = (select auth.uid())
        or exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = conversation_id
              and cp.user_id = (select auth.uid())
        )
    );

create policy "calls_insert" on public.calls
    for insert to authenticated
    with check (caller_id = (select auth.uid()));

create policy "calls_update" on public.calls
    for update to authenticated
    using (
        caller_id = (select auth.uid()) or callee_id = (select auth.uid())
    )
    with check (
        caller_id = (select auth.uid()) or callee_id = (select auth.uid())
    );


-- ── call_signals ─────────────────────────────────────────────
alter table public.call_signals enable row level security;

drop policy if exists "call_signals_select" on public.call_signals;
drop policy if exists "call_signals_insert" on public.call_signals;

create policy "call_signals_select" on public.call_signals
    for select to authenticated
    using (
        exists (
            select 1 from public.calls c
            where c.id = call_id
              and (c.caller_id = (select auth.uid()) or c.callee_id = (select auth.uid()))
        )
    );

create policy "call_signals_insert" on public.call_signals
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1 from public.calls c
            where c.id = call_id
              and (c.caller_id = (select auth.uid()) or c.callee_id = (select auth.uid()))
        )
    );


-- ── invitations ──────────────────────────────────────────────
alter table public.invitations enable row level security;

drop policy if exists "invitations_select" on public.invitations;
drop policy if exists "invitations_insert" on public.invitations;
drop policy if exists "invitations_update" on public.invitations;

create policy "invitations_select" on public.invitations
    for select to authenticated
    using (
        sender_id = (select auth.uid()) or receiver_id = (select auth.uid())
    );

-- Prevent self-invitations and duplicate pending invitations
create policy "invitations_insert" on public.invitations
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and sender_id <> receiver_id
        and not exists (
            select 1 from public.invitations i2
            where i2.sender_id = (select auth.uid())
              and i2.receiver_id = receiver_id
              and i2.status = 'pending'
        )
    );

create policy "invitations_update" on public.invitations
    for update to authenticated
    using (receiver_id = (select auth.uid()))
    with check (receiver_id = (select auth.uid()));


-- ── Storage ──────────────────────────────────────────────────

-- chat-images
drop policy if exists "chat_images_insert" on storage.objects;
create policy "chat_images_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'chat-images'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );

drop policy if exists "chat_images_select" on storage.objects;
create policy "chat_images_select" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'chat-images'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );

-- chat-audio
drop policy if exists "chat_audio_insert" on storage.objects;
create policy "chat_audio_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'chat-audio'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );

drop policy if exists "chat_audio_select" on storage.objects;
create policy "chat_audio_select" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'chat-audio'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );

-- avatars
drop policy if exists "avatars_insert" on storage.objects;
create policy "avatars_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "avatars_update" on storage.objects;
create policy "avatars_update" on storage.objects
    for update to authenticated
    using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    )
    with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "avatars_select" on storage.objects;
create policy "avatars_select" on storage.objects
    for select to authenticated
    using (bucket_id = 'avatars');

-- group-avatars
drop policy if exists "group_avatars_insert" on storage.objects;
create policy "group_avatars_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'group-avatars'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = ((storage.foldername(name))[1])::uuid
              and cp.user_id = (select auth.uid())
              and cp.role = 'admin'
        )
    );

drop policy if exists "group_avatars_update" on storage.objects;
create policy "group_avatars_update" on storage.objects
    for update to authenticated
    using (
        bucket_id = 'group-avatars'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = ((storage.foldername(name))[1])::uuid
              and cp.user_id = (select auth.uid())
              and cp.role = 'admin'
        )
    )
    with check (
        bucket_id = 'group-avatars'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = ((storage.foldername(name))[1])::uuid
              and cp.user_id = (select auth.uid())
              and cp.role = 'admin'
        )
    );

drop policy if exists "group_avatars_select" on storage.objects;
create policy "group_avatars_select" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'group-avatars'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = ((storage.foldername(name))[1])::uuid
              and cp.user_id = (select auth.uid())
        )
    );
