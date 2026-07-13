package com.ajrpachon.chatapp.ui.chat.gallery

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.launch

data class ChatMediaGalleryState(
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
)

sealed interface ChatMediaGalleryEffect

sealed interface ChatMediaGalleryIntent {
    data object Refresh : ChatMediaGalleryIntent
}

class ChatMediaGalleryViewModel(
    private val conversationId: String,
    private val messageDao: MessageDao,
) : BaseViewModel<ChatMediaGalleryState, ChatMediaGalleryEffect>(ChatMediaGalleryState()) {

    init {
        loadMedia()
    }

    fun onIntent(intent: ChatMediaGalleryIntent) {
        when (intent) {
            is ChatMediaGalleryIntent.Refresh -> loadMedia()
        }
    }

    private fun loadMedia() {
        viewModelScope.launch {
            messageDao.getImagesForConversation(conversationId).collect { images ->
                updateState { it.copy(images = images) }
            }
        }
        viewModelScope.launch {
            messageDao.getVideosForConversation(conversationId).collect { videos ->
                updateState { it.copy(videos = videos) }
            }
        }
    }
}
