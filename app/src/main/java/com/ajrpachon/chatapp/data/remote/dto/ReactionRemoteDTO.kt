package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReactionRemoteDTO(
    @SerialName("message_id") val messageId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("emoji") val emoji: String,
)

/** A reaction added or removed by anyone, as received from Supabase Realtime. */
data class ReactionChangeDTO(
    val reaction: ReactionRemoteDTO,
    val removed: Boolean,
)
