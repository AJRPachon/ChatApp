package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class IdOnly(@SerialName("id") val id: String)

@Serializable
private data class ProfileUpsert(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
)

class UserRemoteSource(private val supabase: SupabaseClient) {

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun getProfile(userId: String): UserDTO? = runCatching {
        supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserDTO>()
    }.getOrNull()

    suspend fun upsertProfile(dto: UserDTO) {
        supabase.postgrest["profiles"].upsert(dto)
    }

    suspend fun isUsernameAvailable(username: String): Boolean =
        supabase.postgrest["profiles"]
            .select(Columns.list("id")) { filter { eq("username", username) } }
            .decodeList<IdOnly>()
            .isEmpty()

    suspend fun setUsername(userId: String, username: String): UserDTO {
        // Fallback display name from Google metadata or username itself
        val displayName = supabase.auth.currentUserOrNull()
            ?.userMetadata?.get("full_name")
            ?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: username

        // Upsert handles both: profile missing (trigger failed on first login)
        // and profile already existing (normal update case)
        supabase.postgrest["profiles"].upsert(
            listOf(ProfileUpsert(userId, username, displayName)),
        )
        return supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<UserDTO>()
    }

    suspend fun searchByUsername(query: String): List<UserDTO> =
        supabase.postgrest["profiles"]
            .select { filter { ilike("username", "%$query%") } }
            .decodeList<UserDTO>()
}
