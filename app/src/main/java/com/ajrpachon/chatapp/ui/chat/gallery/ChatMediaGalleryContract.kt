package com.ajrpachon.chatapp.ui.chat.gallery

data class ChatMediaGalleryState(
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
)

sealed interface ChatMediaGalleryEffect

sealed interface ChatMediaGalleryIntent {
    data object Refresh : ChatMediaGalleryIntent
}
