package com.ajrpachon.chatapp.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedMessageItem(
    val id: String,
    val conversationName: String,
    val senderName: String,
    val content: String,
)

data class SavedMessagesState(
    val messages: List<SavedMessageItem> = emptyList(),
)

class SavedMessagesViewModel(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val userDao: UserDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedMessagesState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            messageDao.getSavedMessages().collect { dbos ->
                val items = dbos.map { dbo ->
                    val conversation = conversationDao.getById(dbo.conversationId)
                    val conversationName = conversation?.name?.takeIf { it.isNotBlank() }
                        ?: "Conversación"
                    val sender = userDao.getById(dbo.senderId)
                    val senderName = sender?.displayName?.takeIf { it.isNotBlank() }
                        ?: dbo.senderId.take(8)
                    SavedMessageItem(
                        id = dbo.id,
                        conversationName = conversationName,
                        senderName = senderName,
                        content = dbo.content.ifBlank { "[Archivo adjunto]" },
                    )
                }
                _state.update { it.copy(messages = items) }
            }
        }
    }
}
