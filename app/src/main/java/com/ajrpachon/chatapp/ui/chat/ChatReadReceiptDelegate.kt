package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.util.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Marks incoming messages as read: once when the chat is opened, and again whenever unread
 * messages from other users exist while the chat is on screen (RESUMED). That covers messages
 * that arrive with the conversation already open and the app returning from the background,
 * which previously only got their read receipt after leaving and re-entering the chat. See
 * [ChatAiDelegate] for the delegate pattern this follows.
 */
class ChatReadReceiptDelegate(
    private val conversationId: String,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val scope: CoroutineScope,
) {
    // True only while ChatScreen is RESUMED, so messages that arrive with the app in the
    // background are not marked as read until the user actually sees them.
    private val isScreenResumed = MutableStateFlow(false)

    fun setScreenResumed(resumed: Boolean) {
        isScreenResumed.value = resumed
    }

    fun start(userId: String) {
        scope.launch { markAllAsRead(userId) }
        scope.launch {
            combine(
                isScreenResumed,
                messageRepository.observeUnreadIncomingCount(conversationId, userId),
            ) { resumed, unread -> resumed && unread > 0 }
                .filter { it }
                .collect { markAllAsRead(userId) }
        }
    }

    private suspend fun markAllAsRead(userId: String) {
        catchResult { messageRepository.markAsRead(conversationId, userId) }
        catchResult { conversationRepository.resetUnreadCount(conversationId) }
    }
}
