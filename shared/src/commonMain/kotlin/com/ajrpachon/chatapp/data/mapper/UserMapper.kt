package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.domain.model.UserBO
import kotlinx.datetime.Instant

fun UserDTO.toBO(email: String = "") = UserBO(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = Instant.parse(createdAt),
)

fun UserDTO.toDBO(email: String = "", isCurrentUser: Boolean = false) = UserDBO(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
    isCurrentUser = isCurrentUser,
)

fun UserDBO.toBO() = UserBO(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)
