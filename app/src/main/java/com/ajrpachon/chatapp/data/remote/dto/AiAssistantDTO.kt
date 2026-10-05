package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of a call to the `ai-assistant` Edge Function; [action] selects what it does. */
@Serializable
data class AiRequestDTO(
    val action: String,
    val messages: List<String>? = null,
    val prompt: String? = null,
)

@Serializable
data class AiResponseDTO(val result: String)
