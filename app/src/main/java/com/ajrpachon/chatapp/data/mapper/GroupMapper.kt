package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.remote.dto.GroupMemberDTO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.datetime.Instant

fun GroupMemberDTO.toDBO() = GroupMemberDBO(
    conversationId = conversationId,
    userId = userId,
    displayName = profile?.displayName ?: userId,
    username = profile?.username ?: "",
    avatarUrl = profile?.avatarUrl,
    role = role,
    joinedAt = catchResult { Instant.parse(joinedAt).toEpochMilliseconds() }
        .getOrDefault(System.currentTimeMillis()),
)

fun GroupMemberDBO.toBO() = GroupMemberBO(
    userId = userId,
    conversationId = conversationId,
    displayName = displayName,
    username = username,
    avatarUrl = avatarUrl,
    role = if (role == "admin") GroupRole.ADMIN else GroupRole.MEMBER,
    joinedAt = Instant.fromEpochMilliseconds(joinedAt),
)
