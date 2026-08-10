-- ============================================================
-- video_url column + status-videos storage bucket
--
-- Companion to 20260807092830_add_status_stories.sql: statuses can
-- now be photo or video (StatusBO.videoUrl, Room migration36To37).
-- StatusRemoteSource.uploadStatusVideo uploads to
-- supabase.storage["status-videos"], so the table needs the column
-- and the bucket needs to exist, mirroring the status-images setup.
-- ============================================================

alter table public.user_status add column if not exists video_url text;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'status-videos',
    'status-videos',
    true,
    52428800, -- 50 MB, matches UploadLimits.VIDEO_MAX_BYTES
    array['video/mp4', 'video/3gpp', 'video/webm', 'video/quicktime']
)
on conflict (id) do nothing;

-- Same pattern as status-images: insert restricted to the caller's own
-- folder, select open to any authenticated user (the user_status table
-- RLS from the companion migration is the actual visibility gate).
drop policy if exists "status_videos_insert" on storage.objects;
create policy "status_videos_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'status-videos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "status_videos_select" on storage.objects;
create policy "status_videos_select" on storage.objects
    for select to authenticated
    using (bucket_id = 'status-videos');

drop policy if exists "status_videos_delete" on storage.objects;
create policy "status_videos_delete" on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'status-videos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );
