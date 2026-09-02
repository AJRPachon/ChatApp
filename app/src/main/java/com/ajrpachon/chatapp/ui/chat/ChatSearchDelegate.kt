package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 300L
private const val JUMP_HIGHLIGHT_MS = 2_000L

/**
 * Handles in-conversation message search (debounced) and jumping to/highlighting a message
 * (from search results or a reply reference). Seventh slice of the decomposition in
 * docs/chat-viewmodel-decomposition.md — see [ChatAiDelegate] for the pattern this follows.
 */
class ChatSearchDelegate(
    private val conversationId: String,
    private val currentUserId: () -> String?,
    private val messageRepository: MessageRepository,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val updateState get() = context.updateState
    private var searchJob: Job? = null

    fun searchMessages(query: String) {
        updateState { it.copy(search = it.search.copy(query = query)) }
        searchJob?.cancel()
        if (query.isBlank()) {
            updateState { it.copy(search = it.search.copy(results = emptyList(), isSearching = false)) }
            return
        }
        searchJob = scope.launch {
            updateState { it.copy(search = it.search.copy(isSearching = true)) }
            delay(SEARCH_DEBOUNCE_MS)
            val uid = currentUserId() ?: return@launch
            val results = catchResult { messageRepository.searchMessages(conversationId, uid, query) }.getOrDefault(emptyList())
            updateState { it.copy(search = it.search.copy(results = results, isSearching = false)) }
        }
    }

    fun jumpToMessage(messageId: String) {
        updateState {
            it.copy(search = it.search.copy(isActive = false, query = "", results = emptyList(), highlightedMessageId = messageId))
        }
        scope.launch {
            delay(JUMP_HIGHLIGHT_MS)
            updateState {
                if (it.search.highlightedMessageId == messageId) it.copy(search = it.search.copy(highlightedMessageId = null)) else it
            }
        }
    }
}
