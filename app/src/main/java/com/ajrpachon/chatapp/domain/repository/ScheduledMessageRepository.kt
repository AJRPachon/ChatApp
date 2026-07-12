package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow

interface ScheduledMessageRepository {
    fun observeAll(): Flow<List<ScheduledMessage>>
    suspend fun deleteById(id: String)
    @Suppress("LongParameterList")
    suspend fun schedule(
        id: String,
        conversationId: String,
        senderId: String,
        text: String,
        scheduledAtMs: Long,
        createdAt: Long,
    )
}
