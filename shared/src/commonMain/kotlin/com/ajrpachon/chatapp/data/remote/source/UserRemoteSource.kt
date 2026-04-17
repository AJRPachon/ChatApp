package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class UserRemoteSource(private val supabase: SupabaseClient) {

    suspend fun getProfile(userId: String): UserDTO? = runCatching {
        supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserDTO>()
    }.getOrNull()

    suspend fun upsertProfile(dto: UserDTO) {
        supabase.postgrest["profiles"].upsert(dto)
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val result = supabase.postgrest["profiles"]
            .select(Columns.list("id")) { filter { eq("username", username) } }
            .decodeList<UserDTO>()
        return result.isEmpty()
    }

    suspend fun setUsername(userId: String, username: String): UserDTO =
        supabase.postgrest["profiles"]
            .update({ set("username", username) }) { filter { eq("id", userId) } }
            .decodeSingle<UserDTO>()

    suspend fun searchByUsername(query: String): List<UserDTO> =
        supabase.postgrest["profiles"]
            .select { filter { ilike("username", "%$query%") } }
            .decodeList<UserDTO>()
}
