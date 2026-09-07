package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import com.ajrpachon.chatapp.utils.catchResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class StatusRemoteSource(private val supabase: SupabaseClient) {

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    private val STATUS_IMAGE_BUCKET = "status-images"
    private val statusVideoBucket = "status-videos"

    suspend fun getActiveStatuses(contactIds: List<String>): List<StatusDTO> {
        if (contactIds.isEmpty()) return emptyList()
        val nowIso = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString()
        return supabase.postgrest["user_status"]
            .select {
                filter {
                    gte("expires_at", nowIso)
                }
            }
            .decodeList<StatusDTO>()
            .filter { it.userId in contactIds }
    }

    suspend fun postStatus(dto: StatusDTO) {
        supabase.postgrest["user_status"].insert(dto)
    }

    suspend fun deleteStatus(statusId: String) {
        supabase.postgrest["user_status"].delete { filter { eq("id", statusId) } }
    }

    suspend fun uploadStatusImage(userId: String, bytes: ByteArray): String {
        val path = "$userId/${java.util.UUID.randomUUID()}.jpg"
        supabase.storage[STATUS_IMAGE_BUCKET].upload(path, bytes) { upsert = false }
        return supabase.storage[STATUS_IMAGE_BUCKET].publicUrl(path)
    }

    suspend fun uploadStatusVideo(userId: String, bytes: ByteArray): String {
        val path = "$userId/${java.util.UUID.randomUUID()}.mp4"
        supabase.storage[statusVideoBucket].upload(path, bytes) { upsert = false }
        return supabase.storage[statusVideoBucket].publicUrl(path)
    }

    /**
     * Signals whenever ANY row in `user_status` this user is allowed to see (per the
     * `user_status_select` RLS policy — own rows, plus anyone they share a direct conversation
     * with) changes on the server: someone posts, edits or their status expires/gets deleted.
     * Doesn't carry the row itself — a resync (StatusRepositoryImpl.syncStatuses, which already
     * re-applies the contactIds filter) is cheap and simpler than trying to keep Room in sync
     * from partial Postgres change payloads. Without this, statuses only ever synced once, in
     * StatusViewModel's init{} — a contact's new status never reached a device that already had
     * the conversation list open (root cause of "I post a status and the other person doesn't
     * see it").
     */
    fun observeStatusChanges(userId: String): Flow<Unit> = channelFlow {
        // Refresh token before subscribing so Realtime doesn't use an expired JWT
        catchResult { supabase.auth.refreshCurrentSession() }

        val channel = supabase.realtime.channel("user_status:$userId")

        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "user_status"
        }.onEach { trySend(Unit) }.launchIn(this)

        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "user_status"
        }.onEach { trySend(Unit) }.launchIn(this)

        channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "user_status"
        }.onEach { trySend(Unit) }.launchIn(this)

        channel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { channel.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(channel) }
            }
        }
    }
}
