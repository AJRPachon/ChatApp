-- ============================================================
-- chat-files storage bucket
--
-- Root cause: sending a file attachment fails on-device with
-- "bucket not found", the same bug fixed for chat-videos in
-- 20260728_add_chat_videos_bucket.sql. MessageRepositoryImpl.kt
-- uploads to supabase.storage["chat-files"] (FILE_BUCKET), but the
-- bucket was never created via migration (or, as far as this repo's
-- git history shows, the dashboard either). This migration creates
-- it and adds the same participant-scoped RLS policies used for the
-- other chat-* buckets.
--
-- No allowed_mime_types restriction: UploadLimits.checkFileSize only
-- caps size (50 MB), not file type — the file picker accepts any
-- document type.
-- ============================================================

insert into storage.buckets (id, name, public, file_size_limit)
values (
    'chat-files',
    'chat-files',
    true,
    52428800 -- 50 MB, matches UploadLimits.FILE_MAX_BYTES
)
on conflict (id) do nothing;

drop policy if exists "chat_files_insert" on storage.objects;
create policy "chat_files_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'chat-files'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );

drop policy if exists "chat_files_select" on storage.objects;
create policy "chat_files_select" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'chat-files'
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = (storage.foldername(name))[1]::uuid
              and cp.user_id = (select auth.uid())
        )
    );
