package com.ajrpachon.chatapp.ui.status

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "StatusVM"

data class StatusState(
    val statuses: List<StatusBO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showComposeDialog: Boolean = false,
    val composeText: String = "",
    val selectedColor: Long = 0xFF1976D2,
)

sealed interface StatusIntent {
    data object Refresh : StatusIntent
    data object OpenCompose : StatusIntent
    data object CloseCompose : StatusIntent
    data class TextChanged(val text: String) : StatusIntent
    data class ColorChanged(val color: Long) : StatusIntent
    data object PostTextStatus : StatusIntent
    data class PostImageStatus(val context: Context, val uri: Uri) : StatusIntent
    data class DeleteStatus(val statusId: String) : StatusIntent
}

class StatusViewModel(
    private val statusRepository: StatusRepository,
    private val conversationRepository: ConversationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StatusState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            statusRepository.observeActiveStatuses().collect { statuses ->
                _state.update { it.copy(statuses = statuses) }
            }
        }
        onIntent(StatusIntent.Refresh)
    }

    fun onIntent(intent: StatusIntent) {
        when (intent) {
            is StatusIntent.Refresh -> sync()
            is StatusIntent.OpenCompose -> _state.update { it.copy(showComposeDialog = true, composeText = "", selectedColor = 0xFF1976D2) }
            is StatusIntent.CloseCompose -> _state.update { it.copy(showComposeDialog = false) }
            is StatusIntent.TextChanged -> _state.update { it.copy(composeText = intent.text) }
            is StatusIntent.ColorChanged -> _state.update { it.copy(selectedColor = intent.color) }
            is StatusIntent.PostTextStatus -> postTextStatus()
            is StatusIntent.PostImageStatus -> postImageStatus(intent.context, intent.uri)
            is StatusIntent.DeleteStatus -> deleteStatus(intent.statusId)
        }
    }

    private fun sync() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            catchResult {
                val currentUser = getCurrentUserUseCase().first() ?: return@catchResult
                val contactIds = conversationRepository
                    .observeConversations(currentUser.id)
                    .first()
                    .mapNotNull { if (!it.isGroup) it.participants.firstOrNull { p -> p.id != currentUser.id }?.id else null }
                statusRepository.syncStatuses(contactIds)
            }.onFailure { e -> AppLogger.e(TAG, "sync failed", e) }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun postTextStatus() {
        val text = _state.value.composeText.trim()
        if (text.isBlank()) return
        val color = _state.value.selectedColor
        _state.update { it.copy(showComposeDialog = false) }
        viewModelScope.launch {
            catchResult { statusRepository.postTextStatus(text, color) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun postImageStatus(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            catchResult {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: return@catchResult
                statusRepository.postImageStatus(bytes, null)
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            catchResult { statusRepository.deleteStatus(statusId) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
