package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.InvitationDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun sendInvitation(senderId: String, receiverId: String): InvitationDTO =
        supabase.postgrest["invitations"]
            .insert(mapOf("sender_id" to senderId, "receiver_id" to receiverId))
            .decodeSingle<InvitationDTO>()

    suspend fun updateStatus(invitationId: String, status: String) {
        supabase.postgrest["invitations"]
            .update({ set("status", status) }) { filter { eq("id", invitationId) } }
    }

    fun observeInvitations(userId: String): Flow<InvitationDTO> {
        val channel = supabase.realtime.channel("invitations:$userId")
        return channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "invitations"
                filter = "receiver_id=eq.$userId"
            }
            .map { action ->
                Json.decodeFromString<InvitationDTO>(action.record.toString())
            }
    }
}
