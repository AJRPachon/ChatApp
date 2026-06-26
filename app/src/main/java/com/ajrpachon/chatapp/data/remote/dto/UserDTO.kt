package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("public_key") val publicKey: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("show_online_status") val showOnlineStatus: Boolean = true,
)
