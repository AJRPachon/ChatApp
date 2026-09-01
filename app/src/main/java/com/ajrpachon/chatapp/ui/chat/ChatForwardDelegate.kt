package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles forwarding: a single message (forward dialog) or a multi-select batch (forward
 * selection dialog). Fifth slice of the decomposition in docs/chat-viewmodel-decomposition.md —
 * see [ChatAiDelegate] for the pattern this follows.
 */
class ChatForwardDelegate(
    private val conversationId: String,
    private val currentUserId: () -> String?,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState
    private val sendEffect get() = context.sendEffect

    fun showForwardDialog(message: MessageBO) {
        val uid = currentUserId() ?: return
        scope.launch {
            catchResult {
                val conversations = conversationRepository.observeConversations(uid).first().filter { it.id != conversationId }
                updateState { it.copy(forward = it.forward.copy(showDialog = true, message = message, conversations = conversations)) }
            }.onFailure { updateState { it.copy(error = "No se pudo cargar las conversaciones") } }
        }
    }

    fun forwardMessage(targetConversationId: String) {
        val uid = currentUserId() ?: return
        val message = getState().forward.message ?: return
        updateState { it.copy(forward = it.forward.copy(showDialog = false, message = null, conversations = emptyList())) }
        scope.launch {
            catchResult {
                messageRepository.sendMessage(
                    conversationId = targetConversationId, senderId = uid, content = message.content,
                    imageUrl = message.imageUrl, audioUrl = message.audioUrl, audioDurationMs = message.audioDurationMs,
                    gifUrl = message.gifUrl, stickerUrl = message.stickerUrl,
                )
                sendEffect(ChatEffect.ShowSnackbar("Mensaje reenviado"))
            }.onFailure { updateState { it.copy(error = "No se pudo reenviar") } }
        }
    }

    fun showForwardSelectionDialog() {
        val uid = currentUserId() ?: return
        scope.launch {
            catchResult {
                val conversations = conversationRepository.observeConversations(uid).first().filter { it.id != conversationId }
                updateState { it.copy(forward = it.forward.copy(showSelectionDialog = true, conversations = conversations)) }
            }.onFailure { updateState { it.copy(error = "No se pudo cargar las conversaciones") } }
        }
    }

    fun forwardSelectedMessages(targetConversationId: String) {
        val uid = currentUserId() ?: return
        val ids = getState().selectedMessageIds.toSet()
        updateState {
            it.copy(
                forward = it.forward.copy(showSelectionDialog = false, conversations = emptyList()),
                selectedMessageIds = emptySet(),
            )
        }
        scope.launch {
            val allMessages = catchResult { withContext(Dispatchers.IO) { messageRepository.getAllMessages(conversationId, uid) } }.getOrDefault(emptyList())
            val toForward = allMessages.filter { it.id in ids }
            var forwarded = 0
            for (message in toForward) {
                catchResult {
                    messageRepository.sendMessage(
                        conversationId = targetConversationId, senderId = uid, content = message.content,
                        imageUrl = message.imageUrl, audioUrl = message.audioUrl, audioDurationMs = message.audioDurationMs,
                        gifUrl = message.gifUrl, stickerUrl = message.stickerUrl,
                    )
                    forwarded++
                }
            }
            if (forwarded > 0) sendEffect(ChatEffect.ShowSnackbar("$forwarded mensaje(s) reenviado(s)"))
            else updateState { it.copy(error = "No se pudieron reenviar") }
        }
    }
}
