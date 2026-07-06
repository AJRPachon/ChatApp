package com.ajrpachon.chatapp.ui.saved

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
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
) : BaseViewModel<SavedMessagesState, Nothing>(SavedMessagesState()) {

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
                updateState { it.copy(messages = items) }
            }
        }
    }
}
