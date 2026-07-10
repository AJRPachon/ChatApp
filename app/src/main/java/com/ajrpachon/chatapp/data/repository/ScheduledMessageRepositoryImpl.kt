package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ScheduledMessageDao
import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import com.ajrpachon.chatapp.domain.repository.ScheduledMessageRepository
import kotlinx.coroutines.flow.Flow

class ScheduledMessageRepositoryImpl(
    private val scheduledMessageDao: ScheduledMessageDao,
) : ScheduledMessageRepository {
    override fun observeAll(): Flow<List<ScheduledMessageDBO>> = scheduledMessageDao.observeAll()
    override suspend fun insert(dbo: ScheduledMessageDBO) { scheduledMessageDao.insert(dbo) }
    override suspend fun deleteById(id: String) { scheduledMessageDao.deleteById(id) }
    override suspend fun schedule(
        id: String,
        conversationId: String,
        senderId: String,
        text: String,
        scheduledAtMs: Long,
        createdAt: Long,
    ) {
        scheduledMessageDao.insert(ScheduledMessageDBO(id, conversationId, senderId, text, scheduledAtMs, createdAt))
    }
}
