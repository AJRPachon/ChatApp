package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import kotlinx.coroutines.flow.Flow

interface ScheduledMessageRepository {
    fun observeAll(): Flow<List<ScheduledMessageDBO>>
    suspend fun insert(dbo: ScheduledMessageDBO)
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
