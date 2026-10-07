package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.emoji.EmojiCategoryDTO
import com.ajrpachon.chatapp.domain.model.EmojiCategoryBO

fun EmojiCategoryDTO.toDomain() = EmojiCategoryBO(
    category = category,
    icon = icon,
    emojis = emojis,
)
