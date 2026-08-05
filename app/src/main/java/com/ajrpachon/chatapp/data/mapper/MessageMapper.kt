package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.SendStatus
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
    fileUrl = MediaUrlValidator.sanitize(fileUrl),
    fileName = fileName,
    fileSize = fileSize,
    fileMimeType = fileMimeType,
    videoUrl = MediaUrlValidator.sanitize(videoUrl),
    expiresAt = expiresAt?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
    // Messages arriving from the server are always "sent"
    sendStatus = "sent",
)

fun MessageDBO.toBO(currentUserId: String, senderName: String, senderAvatarUrl: String? = null) = MessageBO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
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
    editedAt = editedAt,
    expiresAt = expiresAt,
    fileUrl = fileUrl,
    fileName = fileName,
    fileSize = fileSize,
    fileMimeType = fileMimeType,
    videoUrl = videoUrl,
    isPinned = isPinned,
    isSaved = isSaved,
    sendStatus = when (sendStatus) {
        "pending" -> SendStatus.PENDING
        "failed" -> SendStatus.FAILED
        else -> SendStatus.SENT
    },
)
