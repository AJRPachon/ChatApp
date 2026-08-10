package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.StatusDBO
import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import com.ajrpachon.chatapp.domain.model.StatusBO
import kotlinx.datetime.Instant

private const val STATUS_TTL_MS = 24 * 60 * 60 * 1000L

fun StatusDTO.toDBO() = StatusDBO(
    id = id,
    userId = userId,
    text = text,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    backgroundColor = backgroundColor,
    createdAt = runCatching { Instant.parse(createdAt).toEpochMilliseconds() }
        .getOrDefault(System.currentTimeMillis()),
    expiresAt = runCatching { Instant.parse(expiresAt).toEpochMilliseconds() }
        .getOrDefault(System.currentTimeMillis() + STATUS_TTL_MS),
)

fun StatusDBO.toBO(
    userName: String,
    userAvatarUrl: String?,
    isFromMe: Boolean,
) = StatusBO(
    id = id,
    userId = userId,
    userName = userName,
    userAvatarUrl = userAvatarUrl,
    text = text,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    backgroundColor = backgroundColor,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    expiresAt = Instant.fromEpochMilliseconds(expiresAt),
    isFromMe = isFromMe,
)
