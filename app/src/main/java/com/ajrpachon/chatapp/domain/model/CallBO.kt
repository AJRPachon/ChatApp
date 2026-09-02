package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class CallBO(
    val id: String,
    val conversationId: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String?,
    val type: CallType,
    val status: CallStatus,
    val roomName: String,
    val createdAt: Instant? = null,
)

/**
 * Prefix LiveKit room names get for group calls (see CallRepositoryImpl.createGroupCall) — the
 * single source of truth for "is this a group call" derived from a room name. Previously
 * re-derived two different ways client-side: MainActivity checked this prefix for incoming
 * calls, while the outgoing-call path used the conversation's own isGroup flag instead of the
 * call itself.
 */
const val GROUP_CALL_ROOM_PREFIX = "group_"

fun CallBO.isGroupCall(): Boolean = roomName.startsWith(GROUP_CALL_ROOM_PREFIX)

