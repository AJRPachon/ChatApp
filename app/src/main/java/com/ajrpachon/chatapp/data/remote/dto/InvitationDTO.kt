package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvitationDTO(
    @SerialName("id") val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("sender") val sender: UserDTO? = null,
)
