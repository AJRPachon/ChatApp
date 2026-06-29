package com.ajrpachon.chatapp.ui.userinfo
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserInfoViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UserInfoState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = catchResult { userRepository.getUserById(userId) }.getOrNull()
            _state.update {
                it.copy(
                    displayName = user?.displayName ?: "",
                    username = user?.username ?: "",
                    avatarUrl = user?.avatarUrl,
                    isLoading = false,
                )
            }
            loadMedia()
        }
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId() ?: return@launch
            val conversation = catchResult {
                conversationRepository.getOrCreateDirectConversation(currentUserId, userId)
            }.getOrNull() ?: return@launch

            messageRepository.observeMessages(conversation.id, currentUserId)
                .onEach { messages ->
                    val urls = messages
                        .flatMap { msg -> listOfNotNull(msg.imageUrl, msg.videoUrl) }
                        .distinct()
                        .reversed()
                    _state.update { it.copy(mediaUrls = urls) }
                }
                .launchIn(viewModelScope)
        }
    }
}
