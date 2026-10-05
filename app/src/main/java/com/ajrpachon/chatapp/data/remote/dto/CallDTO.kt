package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CallDTO(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("caller_id") val callerId: String,
    @SerialName("callee_id") val calleeId: String? = null,
    @SerialName("type") val type: String,
    @SerialName("status") val status: String = "ringing",
    @SerialName("room_name") val roomName: String,
    @SerialName("created_at") val createdAt: String? = null,
)

