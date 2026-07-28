package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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

    suspend fun updateLastSeen(userId: String) {
        runCatching {
            supabase.postgrest["profiles"].update(
                buildJsonObject { put("last_seen", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString()) }
            ) { filter { eq("id", userId) } }
        }
    }

    suspend fun updateShowOnlineStatus(userId: String, show: Boolean) {
        supabase.postgrest["profiles"].update(
            buildJsonObject { put("show_online_status", show) }
        ) { filter { eq("id", userId) } }
    }

    suspend fun updateDisplayName(userId: String, displayName: String) {
        supabase.postgrest["profiles"].update(
            buildJsonObject { put("display_name", displayName) }
        ) { filter { eq("id", userId) } }
    }

    suspend fun searchByEmails(emails: List<String>): List<UserDTO> {
        if (emails.isEmpty()) return emptyList()
        return runCatching {
            supabase.postgrest["profiles"]
                .select { filter { isIn("email", emails) } }
                .decodeList<UserDTO>()
        }.getOrDefault(emptyList())
    }


    // Phone formatting varies per source (spaces/dashes/+), so we fetch profiles and let the
    // caller normalize+compare rather than relying on Postgrest equality on the raw value.
    suspend fun getProfilesWithPhone(): List<UserDTO> = runCatching {
        supabase.postgrest["profiles"]
            .select()
            .decodeList<UserDTO>()
            .filter { !it.phone.isNullOrBlank() }
    }.getOrDefault(emptyList())

    suspend fun getProfileOrThrow(userId: String): UserDTO? =
        supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserDTO>()

    suspend fun uploadAvatar(userId: String, bytes: ByteArray, mimeType: String): String {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val path = "$userId/avatar.$ext"
        supabase.storage["avatars"].upload(path, bytes) { upsert = true }
        val url = supabase.storage["avatars"].publicUrl(path)
        supabase.postgrest["profiles"].update(
            kotlinx.serialization.json.buildJsonObject { put("avatar_url", url) }
        ) { filter { eq("id", userId) } }
        return url
    }

    suspend fun getPublicKey(userId: String): PublicKeyDTO? = runCatching {
        supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<PublicKeyDTO>()
    }.getOrNull()
}

@Serializable
data class PublicKeyDTO(
    @SerialName("id") val id: String,
    @SerialName("public_key") val publicKey: String? = null,
)
