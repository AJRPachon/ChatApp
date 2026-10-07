package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.CallDTO
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import kotlinx.datetime.Instant

fun CallDTO.toBO(callerName: String = "") = CallBO(
    id = id,
    conversationId = conversationId,
    callerId = callerId,
    callerName = callerName,
    calleeId = calleeId,
    type = CallType.fromWire(type),
    status = CallStatus.fromWire(status),
    roomName = roomName,
    createdAt = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
)
