package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.StatusBO
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    fun observeActiveStatuses(): Flow<List<StatusBO>>
    suspend fun syncStatuses(contactIds: List<String>)
    suspend fun postTextStatus(text: String, backgroundColor: Long)
    suspend fun postImageStatus(imageBytes: ByteArray, text: String?)
    suspend fun deleteStatus(statusId: String)
}
