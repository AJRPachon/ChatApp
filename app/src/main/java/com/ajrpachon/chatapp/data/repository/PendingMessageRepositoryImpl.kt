package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.PendingMessageRepository

class PendingMessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
) : PendingMessageRepository {

    override suspend fun savePendingMessage(
        id: String,
        conversationId: String,
        senderId: String,
        content: String,
        replyToId: String?,
        replyToContent: String?,
        replyToSenderName: String?,
    ) {
        val dbo = MessageDBO(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            isRead = true,
            createdAt = System.currentTimeMillis(),
            replyToId = replyToId,
            replyToContent = replyToContent,
            replyToSenderName = replyToSenderName,
            sendStatus = "pending",
        )
        messageDao.upsert(dbo)
    }

    override suspend fun getPendingMessages(): List<MessageBO> {
        val dbos = messageDao.getPendingMessages()
        val senderIds = dbos.map { it.senderId }.distinct()
        val senderMap = userDao.getByIds(senderIds).associateBy { it.id }
        return dbos.map { dbo ->
            val senderName = senderMap[dbo.senderId]?.displayName ?: dbo.senderId
            dbo.toBO(dbo.senderId, senderName, senderMap[dbo.senderId]?.avatarUrl)
        }
    }

    override suspend fun updateSendStatus(messageId: String, status: String) {
        messageDao.updateSendStatus(messageId, status)
    }
}
