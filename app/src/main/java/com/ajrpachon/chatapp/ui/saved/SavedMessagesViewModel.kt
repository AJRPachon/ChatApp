package com.ajrpachon.chatapp.ui.saved
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.launch
data class SavedMessageItem(val id: String, val conversationName: String, val senderName: String, val content: String)
data class SavedMessagesState(val messages: List<SavedMessageItem> = emptyList())
class SavedMessagesViewModel(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
) : BaseViewModel<SavedMessagesState, Nothing>(SavedMessagesState()) {
    init {
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId() ?: ""
            messageRepository.getSavedMessages(currentUserId).collect { messages ->
                val items = messages.map { bo ->
                    val name = conversationRepository.getById(bo.conversationId)?.name?.takeIf { it.isNotBlank() } ?: "Conversacion"
                    SavedMessageItem(id = bo.id, conversationName = name,
                        senderName = bo.senderName.takeIf { it.isNotBlank() } ?: bo.senderId.take(8),
                        content = bo.content.ifBlank { "[Archivo adjunto]" })
                }
                updateState { it.copy(messages = items) }
            }
        }
    }
}