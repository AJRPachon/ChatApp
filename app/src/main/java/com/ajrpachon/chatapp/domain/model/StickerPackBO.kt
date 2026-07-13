package com.ajrpachon.chatapp.domain.model

data class StickerPackBO(
    val id: String,
    val name: String,
    val coverUrl: String,
    val isInstalled: Boolean = false,
)
