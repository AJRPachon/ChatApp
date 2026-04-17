package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val remoteSource: MessageRemoteSource,
    private val currentUserId: String,
) : MessageRepository {

    override fun observeMessages(conversationId: String): Flow<List<MessageBO>> =
        messageDao.observeByConversation(conversationId).map { dbos ->
            dbos.map { dbo ->
                val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
                dbo.toBO(currentUserId, senderName)
            }
        }

    override suspend fun sendMessage(conversationId: String, senderId: String, content: String): MessageBO {
        val dto = MessageDTO(
            id = "",
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            isRead = false,
            createdAt = Clock.System.now().toString(),
        )
        val saved = remoteSource.sendMessage(dto)
        val dbo = saved.toDBO()
        messageDao.upsert(dbo)
        val senderName = userDao.getById(senderId)?.displayName ?: senderId
        return dbo.toBO(currentUserId, senderName)
    }

    override suspend fun markAsRead(conversationId: String, userId: String) {
        messageDao.markAllRead(conversationId)
        remoteSource.markAsRead(conversationId, userId)
    }

    override suspend fun syncMessages(conversationId: String) {
        val dtos = remoteSource.getMessages(conversationId)
        messageDao.upsertAll(dtos.map { it.toDBO() })
    }
}
