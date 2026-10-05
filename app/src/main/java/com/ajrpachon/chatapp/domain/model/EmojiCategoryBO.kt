package com.ajrpachon.chatapp.domain.model

data class EmojiCategoryBO(
    val category: String,
    val icon: String,
    val emojis: List<String>,
)
