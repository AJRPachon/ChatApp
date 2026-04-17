package com.ajrpachon.chatapp.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveConversationsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ──────────────────────────────────────────────────────────────────

data class ConversationListState(
    val conversations: List<ConversationBO> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val error: String? = null,
)

// ── Intents ────────────────────────────────────────────────────────────────

sealed interface ConversationListIntent {
    data class OpenConversation(val conversationId: String) : ConversationListIntent
    data object DismissError : ConversationListIntent
}

// ── Effects ────────────────────────────────────────────────────────────────

sealed interface ConversationListEffect {
    data class NavigateToChat(val conversationId: String) : ConversationListEffect
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class ConversationListViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationListState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ConversationListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().filterNotNull().collectLatest { user ->
                _state.update { it.copy(currentUserId = user.id) }
                observeConversationsUseCase(user.id).collect { convs ->
                    _state.update { it.copy(conversations = convs, isLoading = false) }
                }
            }
        }
    }

    fun onIntent(intent: ConversationListIntent) {
        when (intent) {
            is ConversationListIntent.OpenConversation ->
                viewModelScope.launch { _effect.send(ConversationListEffect.NavigateToChat(intent.conversationId)) }
            is ConversationListIntent.DismissError ->
                _state.update { it.copy(error = null) }
        }
    }
}
