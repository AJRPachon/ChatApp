package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDTO(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ConversationParticipantDTO(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String,
)
