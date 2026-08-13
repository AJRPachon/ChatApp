package com.ajrpachon.chatapp.data.emoji

import kotlinx.serialization.Serializable

@Serializable
data class EmojiCategoryDTO(
    val category: String,
    val icon: String,
    val emojis: List<String>,
)
