-- ============================================================
-- chat-videos storage bucket
--
-- Root cause: sending a video fails on-device with "bucket not
-- found". MessageRepositoryImpl.kt uploads to
-- supabase.storage["chat-videos"] (VIDEO_BUCKET), but unlike
-- chat-images/chat-audio there was never a migration (or, as far
-- as this repo's git history shows, a dashboard-created bucket)
-- for "chat-videos". This migration creates the bucket and mirrors
-- the participant-scoped RLS policies already used for
-- chat-images/chat-audio in 20260625070355_security_rls_hardening.sql.
--
-- NOTE: this repo has no migration precedent for *creating* any of
-- the existing buckets (chat-images, chat-audio, chat-files,
-- avatars, group-avatars) — only for the storage.objects policies
-- on top of them. That strongly suggests those buckets were created
-- out-of-band via the Supabase Dashboard/CLI (`supabase storage`)
-- rather than via SQL migration. The `insert into storage.buckets`
-- below is written to be idempotent (ON CONFLICT DO NOTHING) so it
-- is safe to run whether or not the bucket already exists, but if
-- this project's migrations are not actually applied to production
-- via `supabase db push`/CI, the bucket must also be created
-- manually (dashboard or `supabase storage buckets create
-- chat-videos`) for the "bucket not found" error to actually go away.
-- ============================================================

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'chat-videos',
    'chat-videos',
    true,
    104857600, -- 100 MB, matches UploadLimits.checkVideoSize client-side cap
    array['video/mp4', 'video/3gpp', 'video/webm', 'video/quicktime']
)
on conflict (id) do nothing;

-- chat-videos
drop policy if exists "chat_videos_insert" on storage.objects;
create policy "chat_videos_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'chat-videos'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );

drop policy if exists "chat_videos_select" on storage.objects;
create policy "chat_videos_select" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'chat-videos'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );
