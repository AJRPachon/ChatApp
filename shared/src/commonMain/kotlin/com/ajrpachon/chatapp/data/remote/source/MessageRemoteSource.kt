package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class MessageRemoteSource(private val supabase: SupabaseClient) {

    suspend fun getMessages(conversationId: String): List<MessageDTO> =
        supabase.postgrest["messages"]
            .select { filter { eq("conversation_id", conversationId) } }
            .decodeList<MessageDTO>()

    suspend fun sendMessage(dto: MessageDTO): MessageDTO =
        supabase.postgrest["messages"].insert(dto).decodeSingle<MessageDTO>()

    suspend fun markAsRead(conversationId: String, userId: String) {
        supabase.postgrest["messages"]
            .update({ set("is_read", true) }) {
                filter {
                    eq("conversation_id", conversationId)
                    neq("sender_id", userId)
                    eq("is_read", false)
                }
            }
    }

    fun observeNewMessages(conversationId: String): Flow<MessageDTO> {
        val channel = supabase.realtime.channel("messages:$conversationId")
        return channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter = "conversation_id=eq.$conversationId"
            }
            .map { action ->
                Json.decodeFromString<MessageDTO>(action.record.toString())
            }
    }
}
