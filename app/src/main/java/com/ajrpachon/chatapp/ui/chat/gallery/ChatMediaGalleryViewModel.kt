package com.ajrpachon.chatapp.ui.chat.gallery

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class ChatMediaGalleryState(
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
)

sealed interface ChatMediaGalleryEffect

class ChatMediaGalleryViewModel(
    private val conversationId: String,
    private val messageDao: MessageDao,
) : BaseViewModel<ChatMediaGalleryState, ChatMediaGalleryEffect>(ChatMediaGalleryState()) {

    val images: StateFlow<List<String>> = messageDao
        .getImagesForConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videos: StateFlow<List<String>> = messageDao
        .getVideosForConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
