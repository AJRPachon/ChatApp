package com.ajrpachon.chatapp.domain.model

data class CallBO(
    val id: String,
    val conversationId: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String?,
    val type: CallType,
    val status: CallStatus,
    val roomName: String,
)

