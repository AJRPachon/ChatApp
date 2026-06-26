package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.domain.model.UserBO
import kotlinx.datetime.Instant

fun UserDTO.toBO(email: String = "") = UserBO(
    id = id,
    email = email,
    username = username ?: "",
    displayName = displayName.ifEmpty { email.substringBefore('@') },
    avatarUrl = avatarUrl,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    lastSeen = lastSeen?.let { runCatching { Instant.parse(it) }.getOrNull() },
    showOnlineStatus = showOnlineStatus,
)

fun UserDTO.toDBO(email: String = "", isCurrentUser: Boolean = false) = UserDBO(
    id = id,
    email = email,
    username = username ?: "",
    displayName = displayName.ifEmpty { email.substringBefore('@') },
    avatarUrl = avatarUrl,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt).toEpochMilliseconds()
                else System.currentTimeMillis(),
    isCurrentUser = isCurrentUser,
    lastSeen = lastSeen?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
    showOnlineStatus = showOnlineStatus,
)

fun UserDBO.toBO() = UserBO(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    lastSeen = lastSeen?.let { Instant.fromEpochMilliseconds(it) },
    showOnlineStatus = showOnlineStatus,
)
