package com.ajrpachon.chatapp.data.remote.dto

import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
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

fun CallDTO.toBO(callerName: String = "") = CallBO(
    id = id,
    conversationId = conversationId,
    callerId = callerId,
    callerName = callerName,
    calleeId = calleeId,
    type = if (type == "video") CallType.VIDEO else CallType.AUDIO,
    status = when (status) {
        "active"   -> CallStatus.ACTIVE
        "ended"    -> CallStatus.ENDED
        "rejected" -> CallStatus.REJECTED
        "missed"   -> CallStatus.MISSED
        else       -> CallStatus.RINGING
    },
    roomName = roomName,
)
