package com.ajrpachon.chatapp.ui.status

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
import com.ajrpachon.chatapp.domain.usecase.ReplyToStatusUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.UploadLimits.checkImageSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkVideoSize
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "StatusVM"

class StatusViewModel(
    private val statusRepository: StatusRepository,
    private val conversationRepository: ConversationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val readUriAsBytes: ReadUriAsBytesUseCase,
    private val replyToStatusUseCase: ReplyToStatusUseCase,
) : BaseViewModel<StatusState, StatusEffect>(StatusState()) {

    init {
        viewModelScope.launch {
            val contactIds = contactIds()
            statusRepository.observeActiveStatuses(contactIds).collect { statuses ->
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
            is StatusIntent.PostVideoStatus -> postVideoStatus(intent.uri)
            is StatusIntent.DeleteStatus -> deleteStatus(intent.statusId)
            is StatusIntent.FilterUserStatuses ->
                updateState { it.copy(userStatuses = intent.allStatuses.filter { s -> s.userId == intent.userId }) }
            is StatusIntent.ReplyToStatus -> replyToStatus(intent.status, intent.text)
        }
    }

    private fun replyToStatus(status: StatusBO, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val currentUser = getCurrentUserUseCase().first() ?: return@launch
            replyToStatusUseCase(currentUser.id, status, text)
                .onSuccess { result ->
                    sendEffect(StatusEffect.NavigateToChat(result.conversationId, result.otherUserName))
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Reply to status failed", e)
                    sendEffect(StatusEffect.ShowMessage(e.message ?: "No se pudo enviar la respuesta"))
                }
        }
    }

    private fun sync() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            catchResult { statusRepository.syncStatuses(contactIds()) }
                .onFailure { e -> AppLogger.e(TAG, "sync failed", e) }
            updateState { it.copy(isLoading = false) }
        }
    }

    private suspend fun contactIds(): List<String> {
        val currentUser = getCurrentUserUseCase().first() ?: return emptyList()
        // getLocalConversations only reads Room — right after a fresh login (or a reinstall),
        // that table can still be empty because ConversationListViewModel's own sync hasn't
        // landed yet. Since this contactIds set is captured once and handed to
        // observeActiveStatuses for the lifetime of this ViewModel, a premature empty read here
        // would mean a contact's status never becomes visible for the rest of the session, no
        // matter how the Realtime side of things is doing. Force a real sync first so this
        // reflects the actual server-side conversation list, not whatever Room happened to hold
        // at this exact moment.
        catchResult { conversationRepository.syncConversations(currentUser.id) }
        return conversationRepository
            .getLocalConversations(currentUser.id)
            .mapNotNull { if (!it.isGroup) it.otherUserId else null }
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
                val bytes = readUriAsBytes(uri.toString())
                bytes.checkImageSize()
                statusRepository.postImageStatus(bytes, null)
            }.onFailure { e -> updateState { it.copy(error = e.message) } }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun postVideoStatus(uri: Uri) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            catchResult {
                val bytes = readUriAsBytes(uri.toString())
                bytes.checkVideoSize()
                statusRepository.postVideoStatus(bytes, null)
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
