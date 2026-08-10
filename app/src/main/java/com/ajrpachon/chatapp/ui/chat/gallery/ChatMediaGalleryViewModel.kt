package com.ajrpachon.chatapp.ui.chat.gallery

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.launch

class ChatMediaGalleryViewModel(
    private val conversationId: String,
    private val messageRepository: MessageRepository,
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
            messageRepository.getImagesForConversation(conversationId).collect { images ->
                updateState { it.copy(images = images) }
            }
        }
        viewModelScope.launch {
            messageRepository.getVideosForConversation(conversationId).collect { videos ->
                updateState { it.copy(videos = videos) }
            }
        }
    }
}
