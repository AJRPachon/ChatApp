package com.ajrpachon.chatapp.domain.model

data class ReactionBO(
    val messageId: String,
    val userId: String,
    val emoji: String,
)
