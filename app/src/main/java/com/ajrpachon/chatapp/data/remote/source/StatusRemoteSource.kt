package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Instant

class StatusRemoteSource(private val supabase: SupabaseClient) {

    private val STATUS_IMAGE_BUCKET = "status-images"

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
}
