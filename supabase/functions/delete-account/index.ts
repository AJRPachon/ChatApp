import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from "jsr:@supabase/supabase-js@2"

// ============================================================
// delete-account
//
// Google Play "account deletion" compliance requirement: lets an
// authenticated user permanently delete their ChatApp account and the
// data associated with it. The user to delete is ALWAYS derived from
// the caller's JWT (Authorization header) — never from a client-
// supplied id — so this endpoint can only ever delete the caller's own
// account.
//
// Contract:
//   Request:  POST, no body, Authorization: Bearer <user JWT>
//   Response: 200 { "success": true }
//             4xx/5xx { "error": "<message>" }
//
// Data-handling decisions (see supabase/migrations/ for the schema
// this reasons about — most base tables predate tracked migrations,
// so FK behaviour not called out explicitly below could not be
// confirmed from the migrations directory alone; see the "Known open
// risk" note at the bottom):
//
//   - profiles: NOT deleted explicitly here. 20260625070401_minimal_
//     grants.sql documents "DELETE: not supported — account deletion
//     goes through Auth", i.e. profiles.id -> auth.users(id) is
//     assumed ON DELETE CASCADE and is cleaned up transitively by the
//     final admin.deleteUser() call below.
//   - message_reactions.user_id and fcm_tokens.user_id both have an
//     explicit `REFERENCES auth.users(id) ON DELETE CASCADE`
//     (20260626000003_message_reactions.sql /
//     20260505_fcm_notifications.sql) — no explicit cleanup needed.
//   - user_status.user_id REFERENCES public.profiles(id) ON DELETE
//     CASCADE (20260807092830_add_status_stories.sql), so the *rows*
//     clean up transitively once profiles cascades. Storage objects
//     are NOT covered by any SQL FK though, so the status-images/
//     status-videos folders are removed explicitly below regardless
//     of row-cascade timing.
//   - messages: NOT deleted. Messages are shared conversation history
//     (20260625070401_minimal_grants.sql: "DELETE: not supported —
//     messages are retained for conversation history"). Deleting them
//     would destroy the thread for every other participant of every
//     1:1/group conversation this user was ever in. Instead this
//     reuses the existing soft-delete column (`is_deleted`, added in
//     20260626000001_deleted_messages.sql; MessageBubble.kt renders
//     `DeletedMessageBubble` whenever `isDeleted` is true, ignoring
//     every other field on the message) and additionally scrubs the
//     actual text/attachment columns — stronger than the user-facing
//     "delete one message" flow, which only flips `is_deleted` and
//     intentionally leaves the attachment URL intact (see
//     ChatMessageList.kt's isGroupableImage comment: "imageUrl
//     survives a soft-delete; only isDeleted flips"). This path is a
//     compliance-grade full erasure of this user's content, not a
//     UI-level hide, so it also removes the underlying Storage object
//     for any attachment the user sent.
//   - conversations / conversation_participants: the conversation row
//     itself is left alone (so any other participant keeps their
//     thread + history), but this user's OWN conversation_participants
//     row is deleted for every conversation — the same effect as
//     "leaving" every 1:1 chat and group they were in.
//   - invitations / blocked_users: deleted outright in both directions
//     (sender/receiver, blocker/blocked). Unlike messages these are
//     relationship bookkeeping rows with no "other participant" whose
//     view of shared history would be affected by removing them.
//   - calls / call_signals: rows retained (call history audit trail,
//     same reasoning as messages) but sender/caller/callee are
//     reassigned to the placeholder account below, for the same FK
//     reason as messages/conversations — see next paragraph.
//
// Confirmed via the Supabase SQL editor (pg_constraint against this
// project's actual schema — NOT derivable from supabase/migrations/
// alone, since these base tables predate tracked migrations):
// messages.sender_id, conversations.created_by, calls.caller_id,
// calls.callee_id and call_signals.sender_id are all NOT NULL with a
// hard (NO ACTION, no cascade) FK to profiles/auth.users. Left as-is,
// step 5's admin.deleteUser() would fail with a Postgres FK-violation
// for any user who ever sent a message, created a conversation, or
// made a call — i.e. almost every real account.
//
// Fix: a permanent, fixed-id "deleted user" placeholder account
// (00000000-0000-0000-0000-000000000001, profiles.display_name
// "Usuario eliminado", auth.users banned via ban_duration so it can
// never sign in — created once via the Admin API, not by any
// migration) that every one of those columns gets reassigned to
// below, right before the real account is removed. This reuses the
// existing "look up display name via profiles" rendering path
// everywhere (ChatScreen, CallScreen, etc.) instead of requiring a
// nullable-sender_id UI refactor across the app, and needs no schema
// change — the FK still points at a row, it's just this row now.
const DELETED_USER_PLACEHOLDER_ID = "00000000-0000-0000-0000-000000000001"
// ============================================================

const RATE_LIMIT_WINDOW_MS = 60_000 // 1 minute
const RATE_LIMIT_MAX_REQUESTS = 3   // 3 requests per minute per user — this is a rare,
                                    // destructive action, not a hot path.
const rateLimitMap = new Map<string, number[]>()

function isRateLimited(userId: string): boolean {
  const now = Date.now()
  const timestamps = (rateLimitMap.get(userId) ?? []).filter((t) => now - t < RATE_LIMIT_WINDOW_MS)
  if (timestamps.length >= RATE_LIMIT_MAX_REQUESTS) return true
  timestamps.push(now)
  rateLimitMap.set(userId, timestamps)
  return false
}

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

// Extracts the storage object path from a Supabase public URL, e.g.
// "https://<project>.supabase.co/storage/v1/object/public/chat-images/<conv>/<file>.jpg"
// -> "<conv>/<file>.jpg". Returns null for a null/external URL (e.g. a
// Giphy GIF URL, which is never a Supabase Storage object).
function storagePathFromPublicUrl(url: string | null | undefined, bucket: string): string | null {
  if (!url) return null
  const marker = `/object/public/${bucket}/`
  const idx = url.indexOf(marker)
  if (idx === -1) return null
  return decodeURIComponent(url.slice(idx + marker.length))
}

// Non-empty on purpose: messages_content_check
// (20260728000002_fix_messages_content_check.sql) requires content <> ''
// unless one of the attachment/call columns is set, and this cleanup
// clears all of those. The exact text is never actually shown — the
// client short-circuits on `isDeleted` and renders its own
// DeletedMessageBubble regardless of content.
const DELETED_MESSAGE_PLACEHOLDER = "[deleted]"

Deno.serve(async (req) => {
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405)

  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return jsonResponse({ error: "Unauthorized" }, 401)

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""

  // Identity client: forwards the caller's own JWT so auth.getUser()
  // resolves the user strictly from the token. The rest of this
  // function only ever operates on that resolved id — never on
  // anything a client could pass in a request body.
  const callerClient = createClient(
    supabaseUrl,
    Deno.env.get("SUPABASE_ANON_KEY") ?? "",
    { global: { headers: { Authorization: authHeader } } },
  )
  const { data: { user }, error: authError } = await callerClient.auth.getUser()
  if (authError || !user) return jsonResponse({ error: "Unauthorized" }, 401)

  if (isRateLimited(user.id)) return jsonResponse({ error: "Too many requests" }, 429)

  const userId = user.id

  // Privileged client: service_role bypasses RLS/grants entirely — the
  // only way to touch rows scoped to other participants (e.g. removing
  // this user's own conversation_participants row without needing the
  // is_group_admin()/is_conversation_participant() RLS helpers) and the
  // only way to call auth.admin.deleteUser.
  const admin = createClient(
    supabaseUrl,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
  )

  try {
    // ── 1. Scrub this user's own messages: content + attachments ────
    // Fetch attachment URLs before clearing them — once the columns
    // are nulled the Storage object path can no longer be derived.
    const { data: ownMessages, error: ownMessagesError } = await admin
      .from("messages")
      .select("id, image_url, audio_url, file_url, video_url")
      .eq("sender_id", userId)
      .or("image_url.not.is.null,audio_url.not.is.null,file_url.not.is.null,video_url.not.is.null")

    if (ownMessagesError) throw ownMessagesError

    const removalsByBucket: Record<string, string[]> = {
      "chat-images": [],
      "chat-audio": [],
      "chat-files": [],
      "chat-videos": [],
    }
    for (const m of ownMessages ?? []) {
      const img = storagePathFromPublicUrl(m.image_url, "chat-images")
      const aud = storagePathFromPublicUrl(m.audio_url, "chat-audio")
      const file = storagePathFromPublicUrl(m.file_url, "chat-files")
      const vid = storagePathFromPublicUrl(m.video_url, "chat-videos")
      if (img) removalsByBucket["chat-images"].push(img)
      if (aud) removalsByBucket["chat-audio"].push(aud)
      if (file) removalsByBucket["chat-files"].push(file)
      if (vid) removalsByBucket["chat-videos"].push(vid)
    }

    for (const [bucket, paths] of Object.entries(removalsByBucket)) {
      if (paths.length === 0) continue
      const { error } = await admin.storage.from(bucket).remove(paths)
      // Best-effort: a stale/already-missing object should not block
      // the rest of account deletion.
      if (error) console.error(`storage.remove(${bucket}) failed`, error)
    }

    // sender_id is reassigned to the placeholder in this same update —
    // not a separate pass — so this filter (`eq("sender_id", userId)`)
    // still matches every row that's actually this user's, regardless
    // of statement ordering.
    const { error: scrubError } = await admin
      .from("messages")
      .update({
        is_deleted: true,
        content: DELETED_MESSAGE_PLACEHOLDER,
        sender_id: DELETED_USER_PLACEHOLDER_ID,
        image_url: null,
        audio_url: null,
        audio_duration_ms: null,
        gif_url: null,
        sticker_url: null,
        file_url: null,
        file_name: null,
        file_size: null,
        file_mime_type: null,
        video_url: null,
      })
      .eq("sender_id", userId)
    if (scrubError) throw scrubError

    // ── 2. Remove this user's own Storage folders ────────────────────
    // avatars/status-images/status-videos are keyed by "$userId/...".
    // chat-images/chat-audio/chat-files/chat-videos are keyed by
    // conversation id and shared with other participants — those
    // buckets are handled per-file above via the messages table, never
    // wiped wholesale.
    for (const bucket of ["avatars", "status-images", "status-videos"]) {
      const { data: files, error: listError } = await admin.storage.from(bucket).list(userId)
      if (listError) {
        console.error(`storage.list(${bucket}) failed`, listError)
        continue
      }
      const paths = (files ?? []).map((f) => `${userId}/${f.name}`)
      if (paths.length === 0) continue
      const { error: removeError } = await admin.storage.from(bucket).remove(paths)
      if (removeError) console.error(`storage.remove(${bucket}) failed`, removeError)
    }

    // ── 3. Relationship/bookkeeping rows — safe to delete outright,
    // no other participant's view of shared history depends on them.
    const { error: participantsError } = await admin
      .from("conversation_participants")
      .delete()
      .eq("user_id", userId)
    if (participantsError) throw participantsError

    const { error: invitationsError } = await admin
      .from("invitations")
      .delete()
      .or(`sender_id.eq.${userId},receiver_id.eq.${userId}`)
    if (invitationsError) throw invitationsError

    const { error: blockedError } = await admin
      .from("blocked_users")
      .delete()
      .or(`blocker_id.eq.${userId},blocked_id.eq.${userId}`)
    if (blockedError) throw blockedError

    // ── 4. user_status rows: already ON DELETE CASCADE from profiles,
    // deleted explicitly too for defense-in-depth (the Storage files
    // above are already gone regardless of row-cascade timing).
    const { error: statusError } = await admin
      .from("user_status")
      .delete()
      .eq("user_id", userId)
    if (statusError) throw statusError

    // ── 4.5. Reassign the remaining hard (NOT NULL, NO ACTION) FK
    // references to the "deleted user" placeholder — see the header
    // comment. This must happen before step 5's deleteUser() call, or
    // that call fails on any of these still pointing at the real user.
    const { error: conversationsError } = await admin
      .from("conversations")
      .update({ created_by: DELETED_USER_PLACEHOLDER_ID })
      .eq("created_by", userId)
    if (conversationsError) throw conversationsError

    const { error: callerError } = await admin
      .from("calls")
      .update({ caller_id: DELETED_USER_PLACEHOLDER_ID })
      .eq("caller_id", userId)
    if (callerError) throw callerError

    const { error: calleeError } = await admin
      .from("calls")
      .update({ callee_id: DELETED_USER_PLACEHOLDER_ID })
      .eq("callee_id", userId)
    if (calleeError) throw calleeError

    const { error: callSignalsError } = await admin
      .from("call_signals")
      .update({ sender_id: DELETED_USER_PLACEHOLDER_ID })
      .eq("sender_id", userId)
    if (callSignalsError) throw callSignalsError

    // ── 5. The actual account deletion — the only step that removes
    // the row from auth.users. Everything above is cleanup of data
    // that either isn't covered by a DB-level cascade, or (messages)
    // is intentionally preserved/anonymized rather than cascaded.
    const { error: deleteUserError } = await admin.auth.admin.deleteUser(userId)
    if (deleteUserError) throw deleteUserError

    return jsonResponse({ success: true }, 200)
  } catch (e) {
    console.error("delete-account failed for user", userId, e)
    return jsonResponse({ error: e instanceof Error ? e.message : String(e) }, 500)
  }
})
