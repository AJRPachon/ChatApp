-- ============================================================
-- user_status table + status-images bucket
--
-- Root cause: the status/stories UI (StatusScreen, StatusViewModel,
-- StatusRepositoryImpl, StatusRemoteSource) and the local Room table
-- (migration19To20 in DatabaseBuilder.kt) were built client-side, but
-- the corresponding Supabase table/bucket were never created. Every
-- call to StatusRemoteSource.getActiveStatuses/postStatus fails with
-- PGRST205 "Could not find the table 'public.user_status'", so no
-- status — own or a contact's — ever reaches the device.
--
-- Visibility model mirrors what the Android client already assumes
-- (StatusViewModel.sync computes contactIds from direct-conversation
-- partners only): a user can see their own statuses plus those of
-- anyone they share a non-group conversation with.
-- ============================================================

create table public.user_status (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    text text,
    image_url text,
    background_color bigint not null default 4279858898, -- 0xFF1976D2
    created_at timestamptz not null default now(),
    expires_at timestamptz not null
);

create index user_status_expires_at_idx on public.user_status (expires_at);
create index user_status_user_id_idx on public.user_status (user_id);

alter table public.user_status enable row level security;

-- SECURITY DEFINER helper: RLS on conversation_participants only lets a
-- user see their own participant rows, so a plain EXISTS/join subquery
-- inside the user_status policy would never see the other user's row.
-- Same bypass pattern as public.is_conversation_participant (see
-- 20260628000004_fix_participants_rls_recursion.sql).
create or replace function public.users_share_direct_conversation(
    user_a uuid,
    user_b uuid
)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
    select exists (
        select 1
        from public.conversation_participants cp1
        join public.conversation_participants cp2
          on cp1.conversation_id = cp2.conversation_id
        join public.conversations c on c.id = cp1.conversation_id
        where cp1.user_id = user_a
          and cp2.user_id = user_b
          and c.is_group = false
    );
$$;

-- Postgres grants EXECUTE to PUBLIC on new functions by default, which
-- would make this callable directly via /rest/v1/rpc/... by anon and
-- authenticated alike. It's only meant to be invoked from inside the
-- policy below, so lock it down to authenticated.
revoke execute on function public.users_share_direct_conversation(uuid, uuid) from public;
grant execute on function public.users_share_direct_conversation(uuid, uuid) to authenticated;

create policy "user_status_select" on public.user_status
    for select to authenticated
    using (
        user_id = (select auth.uid())
        or public.users_share_direct_conversation((select auth.uid()), user_id)
    );

create policy "user_status_insert" on public.user_status
    for insert to authenticated
    with check (user_id = (select auth.uid()));

create policy "user_status_delete" on public.user_status
    for delete to authenticated
    using (user_id = (select auth.uid()));

-- Least-privilege GRANT — without this, Postgres denies access before
-- RLS is evaluated (see 20260625070401_minimal_grants.sql / the
-- blocked_users fix in 20260728000010_grant_blocked_users.sql).
revoke all on public.user_status from authenticated;
grant select, insert, delete on public.user_status to authenticated;

-- ── Storage: status-images bucket ───────────────────────────────
-- StatusRemoteSource.uploadStatusImage stores under "$userId/uuid.jpg"
-- and reads back a public URL, so the bucket must be public. Insert is
-- restricted to the caller's own folder; select is open to any
-- authenticated user (same pattern as the avatars bucket) since the
-- user_status table policy above is the real visibility gate.
insert into storage.buckets (id, name, public, file_size_limit)
values (
    'status-images',
    'status-images',
    true,
    10485760 -- 10 MB, matches UploadLimits.IMAGE_MAX_BYTES
)
on conflict (id) do nothing;

drop policy if exists "status_images_insert" on storage.objects;
create policy "status_images_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'status-images'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "status_images_select" on storage.objects;
create policy "status_images_select" on storage.objects
    for select to authenticated
    using (bucket_id = 'status-images');

drop policy if exists "status_images_delete" on storage.objects;
create policy "status_images_delete" on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'status-images'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );
