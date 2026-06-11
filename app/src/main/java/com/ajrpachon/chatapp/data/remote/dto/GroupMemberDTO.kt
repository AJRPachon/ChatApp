package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberDTO(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String = "member",
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("profiles") val profile: GroupMemberProfileDTO? = null,
)

@Serializable
data class GroupMemberProfileDTO(
    @SerialName("id") val id: String = "",
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
