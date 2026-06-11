package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDTO(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("is_group") val isGroup: Boolean = false,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("description") val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class ConversationParticipantDTO(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String = "member",
    @SerialName("joined_at") val joinedAt: String,
)

// Used for the sync query that embeds the conversation row
@Serializable
data class ConversationParticipantWithConvDTO(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("conversations") val conversation: ConversationDTO,
)
