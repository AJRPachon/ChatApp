package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.repository.AiAssistantRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles the AI-assistant sheet (summarize / suggest-reply / freeform prompt) — the first
 * concern extracted out of ChatViewModel's flat method list. See
 * docs/chat-viewmodel-decomposition.md for the migration plan this follows.
 *
 * [ChatState]/[ChatIntent] are unchanged: this delegate only ever produces a new state via
 * [context]'s `updateState` (bound to `BaseViewModel.updateState`), the same `.copy()` pattern
 * every intent handler on ChatViewModel itself uses — so ChatScreen needs no changes.
 */
class ChatAiDelegate(
    private val conversationId: String,
    private val currentUserId: () -> String?,
    private val messageRepository: MessageRepository,
    private val aiAssistantRepository: AiAssistantRepository,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val updateState get() = context.updateState

    fun summarize() {
        val uid = currentUserId() ?: return
        updateState { it.copy(isAiLoading = true) }
        scope.launch {
            val snippets = catchResult {
                messageRepository.getAllMessages(conversationId, uid).takeLast(AI_CONTEXT_MESSAGE_COUNT).map { it.content }
            }.getOrDefault(emptyList())
            aiAssistantRepository.summarize(snippets)
                .onSuccess { result -> updateState { it.copy(aiSuggestion = result, isAiLoading = false) } }
                .onFailure { e -> updateState { it.copy(isAiLoading = false, error = e.message) } }
        }
    }

    fun suggestReply() {
        val uid = currentUserId() ?: return
        updateState { it.copy(isAiLoading = true) }
        scope.launch {
            val last = catchResult {
                messageRepository.getAllMessages(conversationId, uid).lastOrNull { it.senderId != uid }?.content ?: ""
            }.getOrDefault("")
            aiAssistantRepository.suggestReply(last)
                .onSuccess { result -> updateState { it.copy(aiSuggestion = result, isAiLoading = false) } }
                .onFailure { e -> updateState { it.copy(isAiLoading = false, error = e.message) } }
        }
    }

    fun freeform(prompt: String) {
        updateState { it.copy(isAiLoading = true) }
        scope.launch {
            aiAssistantRepository.freeform(prompt)
                .onSuccess { result -> updateState { it.copy(aiSuggestion = result, isAiLoading = false) } }
                .onFailure { e -> updateState { it.copy(isAiLoading = false, error = e.message) } }
        }
    }

    companion object {
        private const val AI_CONTEXT_MESSAGE_COUNT = 20
    }
}
