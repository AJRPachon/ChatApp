package com.ajrpachon.chatapp.ui.status

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "StatusVM"

class StatusViewModel(
    private val statusRepository: StatusRepository,
    private val conversationRepository: ConversationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val readUriAsBytes: ReadUriAsBytesUseCase,
) : BaseViewModel<StatusState, StatusEffect>(StatusState()) {

    init {
        viewModelScope.launch {
            statusRepository.observeActiveStatuses().collect { statuses ->
                updateState { it.copy(statuses = statuses) }
            }
        }
        onIntent(StatusIntent.Refresh)
    }

    fun onIntent(intent: StatusIntent) {
        when (intent) {
            is StatusIntent.Refresh -> sync()
            is StatusIntent.OpenCompose -> updateState { it.copy(showComposeDialog = true, composeText = "", selectedColor = 0xFF1976D2) }
            is StatusIntent.CloseCompose -> updateState { it.copy(showComposeDialog = false) }
            is StatusIntent.TextChanged -> updateState { it.copy(composeText = intent.text) }
            is StatusIntent.ColorChanged -> updateState { it.copy(selectedColor = intent.color) }
            is StatusIntent.PostTextStatus -> postTextStatus()
            is StatusIntent.PostImageStatus -> postImageStatus(intent.uri)
            is StatusIntent.DeleteStatus -> deleteStatus(intent.statusId)
            is StatusIntent.FilterUserStatuses ->
                updateState { it.copy(userStatuses = intent.allStatuses.filter { s -> s.userId == intent.userId }) }
        }
    }

    private fun sync() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            catchResult {
                val currentUser = getCurrentUserUseCase().first() ?: return@catchResult
                val contactIds = conversationRepository
                    .observeConversations(currentUser.id)
                    .first()
                    .mapNotNull { if (!it.isGroup) it.participants.firstOrNull { p -> p.id != currentUser.id }?.id else null }
                statusRepository.syncStatuses(contactIds)
            }.onFailure { e -> AppLogger.e(TAG, "sync failed", e) }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun postTextStatus() {
        val text = state.value.composeText.trim()
        if (text.isBlank()) return
        val color = state.value.selectedColor
        updateState { it.copy(showComposeDialog = false) }
        viewModelScope.launch {
            catchResult { statusRepository.postTextStatus(text, color) }
                .onFailure { e -> updateState { it.copy(error = e.message) } }
        }
    }

    private fun postImageStatus(uri: Uri) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            catchResult {
                val bytes = readUriAsBytes(uri)
                statusRepository.postImageStatus(bytes, null)
            }.onFailure { e -> updateState { it.copy(error = e.message) } }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            catchResult { statusRepository.deleteStatus(statusId) }
                .onFailure { e -> updateState { it.copy(error = e.message) } }
        }
    }
}
