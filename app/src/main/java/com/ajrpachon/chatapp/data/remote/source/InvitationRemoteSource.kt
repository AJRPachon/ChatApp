package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.InvitationDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class InvitationRemoteSource(private val supabase: SupabaseClient) {

    suspend fun getPendingInvitations(userId: String): List<InvitationDTO> =
        supabase.postgrest["invitations"]
            .select(Columns.raw("*, sender:profiles!sender_id(*)")) {
                filter {
                    eq("receiver_id", userId)
                    eq("status", "pending")
                }
            }
            .decodeList<InvitationDTO>()

    suspend fun sendInvitation(senderId: String, receiverId: String): InvitationDTO {
        // If a previous rejected invitation exists, reset it to pending
        val existing = supabase.postgrest["invitations"]
            .select {
                filter {
                    eq("sender_id", senderId)
                    eq("receiver_id", receiverId)
                    eq("status", "rejected")
                }
            }
            .decodeList<InvitationDTO>()
            .firstOrNull()

        if (existing != null) {
            supabase.postgrest["invitations"]
                .update({ set("status", "pending") }) {
                    filter { eq("id", existing.id) }
                }
            return existing.copy(status = "pending")
        }

        return supabase.postgrest["invitations"]
            .insert(mapOf("sender_id" to senderId, "receiver_id" to receiverId))
            .decodeSingle<InvitationDTO>()
    }

    suspend fun updateStatus(invitationId: String, status: String) {
        supabase.postgrest["invitations"]
            .update({ set("status", status) }) { filter { eq("id", invitationId) } }
    }

    // Returns all invitations between these two users in either direction (any status)
    suspend fun getRelationshipInvitations(userA: String, userB: String): List<InvitationDTO> =
        supabase.postgrest["invitations"]
            .select {
                filter {
                    or {
                        and {
                            eq("sender_id", userA)
                            eq("receiver_id", userB)
                        }
                        and {
                            eq("sender_id", userB)
                            eq("receiver_id", userA)
                        }
                    }
                }
            }
            .decodeList<InvitationDTO>()

    suspend fun blockUser(blockerId: String, blockedId: String) {
        supabase.postgrest["blocked_users"]
            .insert(mapOf("blocker_id" to blockerId, "blocked_id" to blockedId))
    }

    suspend fun unblockUser(blockerId: String, blockedId: String) {
        supabase.postgrest["blocked_users"]
            .delete { filter { eq("blocker_id", blockerId); eq("blocked_id", blockedId) } }
    }

    suspend fun isBlocked(userA: String, userB: String): Boolean {
        val rows = supabase.postgrest["blocked_users"]
            .select {
                filter {
                    or {
                        and { eq("blocker_id", userA); eq("blocked_id", userB) }
                        and { eq("blocker_id", userB); eq("blocked_id", userA) }
                    }
                }
            }
            .decodeList<Map<String, String>>()
        return rows.isNotEmpty()
    }

    fun observeInvitations(userId: String): Flow<InvitationDTO> = channelFlow {
        // Refresh token before subscribing so Realtime doesn't use an expired JWT
        catchResult { supabase.auth.refreshCurrentSession() }

        val channel = supabase.realtime.channel("invitations:$userId")

        fun tryEmit(record: String) {
            catchResult {
                val dto = Json.decodeFromString<InvitationDTO>(record)
                if (dto.receiverId == userId && dto.status == "pending") trySend(dto)
            }
        }

        // INSERT: nueva invitación
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "invitations"
        }.onEach { tryEmit(it.record.toString()) }.launchIn(this)

        // UPDATE: invitación re-enviada tras rechazo previo (upsert genera UPDATE)
        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "invitations"
        }.onEach { tryEmit(it.record.toString()) }.launchIn(this)

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
