package com.ajrpachon.chatapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveMessagesUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ──────────────────────────────────────────────────────────────────

data class ChatState(
    val messages: List<MessageBO> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val currentUserId: String? = null,
    val error: String? = null,
)

// ── Intents ────────────────────────────────────────────────────────────────

sealed interface ChatIntent {
    data class InputChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data object DismissError : ChatIntent
}

// ── Effects ────────────────────────────────────────────────────────────────

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class ChatViewModel(
    private val conversationId: String,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().filterNotNull().first()
            _state.update { it.copy(currentUserId = user.id) }
            observeMessagesUseCase(conversationId).collect { msgs ->
                _state.update { it.copy(messages = msgs) }
                if (msgs.isNotEmpty()) _effect.send(ChatEffect.ScrollToBottom)
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.Send -> sendMessage()
            is ChatIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        val userId = _state.value.currentUserId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, inputText = "") }
            sendMessageUseCase(conversationId, userId, text)
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, inputText = text) }
                }
            _state.update { it.copy(isSending = false) }
        }
    }
}
