package com.ajrpachon.chatapp.domain.model

data class StickerBO(
    val id: String,
    val packId: String,
    val imageUrl: String,
    val tags: String = "",
)
