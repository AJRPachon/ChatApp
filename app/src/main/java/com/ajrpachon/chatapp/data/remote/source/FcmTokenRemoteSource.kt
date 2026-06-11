package com.ajrpachon.chatapp.data.remote.source

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FcmTokenRemoteSource(private val supabase: SupabaseClient) {

    suspend fun upsertToken(token: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["fcm_tokens"].upsert(
            buildJsonObject {
                put("user_id", userId)
                put("token", token)
                put("updated_at", java.time.Instant.now().toString())
            }
        )
    }

    suspend fun deleteToken(token: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["fcm_tokens"].delete {
            filter {
                eq("user_id", userId)
                eq("token", token)
            }
        }
    }
}
