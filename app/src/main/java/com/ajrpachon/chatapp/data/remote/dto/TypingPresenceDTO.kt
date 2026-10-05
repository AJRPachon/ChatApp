package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.Serializable

/** The presence payload each client tracks on a conversation's `typing-<id>` Realtime channel. */
@Serializable
data class TypingPresenceDTO(
    val isTyping: Boolean,
    val userId: String,
    val userName: String,
)
