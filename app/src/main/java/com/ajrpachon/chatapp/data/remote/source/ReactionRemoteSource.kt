package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.ReactionRemoteDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ReactionRemoteSource(private val supabase: SupabaseClient) {

    suspend fun deleteReaction(messageId: String, userId: String, emoji: String) {
        supabase.postgrest["message_reactions"].delete {
            filter {
                eq("message_id", messageId)
                eq("user_id", userId)
                eq("emoji", emoji)
            }
        }
    }

    suspend fun insertReaction(messageId: String, userId: String, emoji: String) {
        supabase.postgrest["message_reactions"].insert(
            ReactionRemoteDTO(messageId, userId, emoji)
        )
    }
}
