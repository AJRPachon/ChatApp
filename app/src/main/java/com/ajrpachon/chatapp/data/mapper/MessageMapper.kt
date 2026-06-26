package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.datetime.Instant

fun MessageDTO.toDBO() = MessageDBO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = isRead,
    createdAt = runCatching { Instant.parse(createdAt).toEpochMilliseconds() }
        .getOrDefault(System.currentTimeMillis()),
    imageUrl = MediaUrlValidator.sanitize(imageUrl),
    audioUrl = MediaUrlValidator.sanitize(audioUrl),
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToSenderName = replyToSenderName,
    callType = callType,
    callStatus = callStatus,
    callDuration = callDuration,
    gifUrl = MediaUrlValidator.sanitize(gifUrl),
    stickerUrl = stickerUrl,
    isEncrypted = isEncrypted,
    isDeleted = isDeleted,
    isEdited = isEdited,
    editedAt = editedAt?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
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
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToSenderName = replyToSenderName,
    callType = callType,
    callStatus = callStatus,
    callDuration = callDuration,
    gifUrl = gifUrl,
    stickerUrl = stickerUrl,
    isEncrypted = isEncrypted,
    isDeleted = isDeleted,
    isEdited = isEdited,
)
