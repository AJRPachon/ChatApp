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

