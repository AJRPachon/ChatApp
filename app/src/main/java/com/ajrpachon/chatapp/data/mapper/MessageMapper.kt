package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.OutgoingMessageBO
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
    audioDurationMs = audioDurationMs,
    audioAmplitudes = audioAmplitudes,
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
    replyToStatusId = replyToStatusId,
    replyToStatusOwnerId = replyToStatusOwnerId,
    replyToStatusText = replyToStatusText,
    replyToStatusImageUrl = MediaUrlValidator.sanitize(replyToStatusImageUrl),
    replyToStatusVideoUrl = MediaUrlValidator.sanitize(replyToStatusVideoUrl),
    replyToStatusBackgroundColor = replyToStatusBackgroundColor,
    replyToStatusExpiresAt = replyToStatusExpiresAt?.let {
        runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull()
    },
)

/**
 * Builds the row sent to Supabase for [this] outgoing message. [content] is passed separately
 * because it may be the E2EE ciphertext instead of [OutgoingMessageBO.content]; [id] and [createdAt]
 * are supplied by the caller so the mapping itself stays deterministic.
 */
fun OutgoingMessageBO.toDTO(id: String, createdAt: String, content: String, isEncrypted: Boolean) = MessageDTO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = false,
    createdAt = createdAt,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    audioDurationMs = audioDurationMs,
    audioAmplitudes = audioAmplitudes,
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToSenderName = replyToSenderName,
    callType = callType,
    callStatus = callStatus,
    callDuration = callDuration,
    gifUrl = gifUrl,
    stickerUrl = stickerUrl,
    fileUrl = fileUrl,
    fileName = fileName,
    fileSize = fileSize,
    fileMimeType = fileMimeType,
    videoUrl = videoUrl,
    isEncrypted = isEncrypted,
    replyToStatusId = statusReply?.statusId,
    replyToStatusOwnerId = statusReply?.statusOwnerId,
    replyToStatusText = statusReply?.statusText,
    replyToStatusImageUrl = statusReply?.statusImageUrl,
    replyToStatusVideoUrl = statusReply?.statusVideoUrl,
    replyToStatusBackgroundColor = statusReply?.statusBackgroundColor,
    replyToStatusExpiresAt = statusReply?.statusExpiresAt?.let { Instant.fromEpochMilliseconds(it).toString() },
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
    audioDurationMs = audioDurationMs,
    audioAmplitudes = audioAmplitudes,
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
    replyToStatusId = replyToStatusId,
    replyToStatusOwnerId = replyToStatusOwnerId,
    replyToStatusText = replyToStatusText,
    replyToStatusImageUrl = replyToStatusImageUrl,
    replyToStatusVideoUrl = replyToStatusVideoUrl,
    replyToStatusBackgroundColor = replyToStatusBackgroundColor,
    replyToStatusExpiresAt = replyToStatusExpiresAt,
)
