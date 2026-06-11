package com.ajrpachon.chatapp.ui.conversations
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveConversationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class ConversationListViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val observeInvitationsUseCase: ObserveInvitationsUseCase,
    private val conversationRepository: ConversationRepository,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationListState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ConversationListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch { fcmTokenManager.syncToken() }
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                _state.update { it.copy(currentUserId = user.id, isLoading = false) }
                supervisorScope {
                    launch {
                        observeInvitationsUseCase(user.id).collect { invitations ->
                            _state.update { it.copy(pendingInvitationsCount = invitations.size) }
                        }
                    }
                    launch {
                        observeConversationsUseCase(user.id).collect { convs ->
                            _state.update { it.copy(conversations = convs) }
                        }
                    }
                }
            }.onFailure { e ->
                AppLogger.e("ConversationListViewModel", "Observe conversations failed", e)
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: ConversationListIntent) {
        when (intent) {
            is ConversationListIntent.OpenConversation ->
                _effect.trySend(ConversationListEffect.NavigateToChat(intent.conversationId, intent.conversationName, intent.isGroup))
            is ConversationListIntent.DismissError ->
                _state.update { it.copy(error = null) }
            is ConversationListIntent.DeleteConversation ->
                viewModelScope.launch {
                    catchResult { conversationRepository.deleteConversation(intent.conversationId) }
                        .onFailure { e -> _state.update { it.copy(error = e.message) } }
                }
            is ConversationListIntent.ToggleMute ->
                viewModelScope.launch {
                    catchResult { conversationRepository.toggleMute(intent.conversationId, intent.muted) }
                        .onFailure { e -> _state.update { it.copy(error = e.message) } }
                }
            is ConversationListIntent.ClearChat ->
                viewModelScope.launch {
                    catchResult { conversationRepository.clearChat(intent.conversationId) }
                        .onFailure { e -> _state.update { it.copy(error = e.message) } }
                }
            is ConversationListIntent.LeaveGroup ->
                viewModelScope.launch {
                    val userId = _state.value.currentUserId ?: return@launch
                    leaveGroupUseCase(intent.conversationId, userId)
                        .onFailure { e -> _state.update { it.copy(error = e.message) } }
                }
        }
    }
}
