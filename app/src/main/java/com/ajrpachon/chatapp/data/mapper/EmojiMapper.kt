package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.emoji.EmojiCategoryDTO
import com.ajrpachon.chatapp.domain.model.EmojiCategory

fun EmojiCategoryDTO.toDomain() = EmojiCategory(
    category = category,
    icon = icon,
    emojis = emojis,
)
