package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.datetime.Instant

fun MessageDTO.toDBO() = MessageDBO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = isRead,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
)

fun MessageDBO.toBO(currentUserId: String, senderName: String) = MessageBO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    content = content,
    isRead = isRead,
    isFromMe = senderId == currentUserId,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)
