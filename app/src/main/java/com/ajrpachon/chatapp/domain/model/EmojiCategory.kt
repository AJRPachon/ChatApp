package com.ajrpachon.chatapp.domain.model

data class EmojiCategory(
    val category: String,
    val icon: String,
    val emojis: List<String>,
)
