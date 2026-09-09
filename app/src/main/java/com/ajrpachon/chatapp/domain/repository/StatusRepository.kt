package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.StatusBO
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    /**
     * [contactIds] are the users (besides the caller) whose statuses should be visible — same
     * set [syncStatuses] takes. Also subscribes to Supabase Realtime changes on `user_status` for
     * as long as the flow is collected, so a status posted by any of them while this is being
     * observed reaches the local DB (and this flow) without a manual resync.
     */
    fun observeActiveStatuses(contactIds: List<String>): Flow<List<StatusBO>>
    suspend fun syncStatuses(contactIds: List<String>)
    suspend fun postTextStatus(text: String, backgroundColor: Long)
    suspend fun postImageStatus(imageBytes: ByteArray, text: String?)
    suspend fun postVideoStatus(videoBytes: ByteArray, text: String?)
    suspend fun deleteStatus(statusId: String)
}
