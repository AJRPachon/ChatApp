package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDTO(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("text") val text: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("background_color") val backgroundColor: Long = 0xFF1976D2,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
)
