package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ScheduledMessageBO
import kotlinx.coroutines.flow.Flow

interface ScheduledMessageRepository {
    fun observeAll(): Flow<List<ScheduledMessageBO>>
    suspend fun deleteById(id: String)
    suspend fun getPending(nowMs: Long): List<ScheduledMessageBO>
    suspend fun schedule(
        id: String,
        conversationId: String,
        senderId: String,
        text: String,
        scheduledAtMs: Long,
        createdAt: Long,
    )
}
