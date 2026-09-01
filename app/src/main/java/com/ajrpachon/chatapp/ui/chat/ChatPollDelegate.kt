package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "ChatPollDelegate"

/**
 * Handles polls: observing a poll's live question/options/current-user-vote for
 * [ChatState.pollUiStates], creating a poll, and voting. Third slice of the decomposition in
 * docs/chat-viewmodel-decomposition.md — see [ChatAiDelegate] for the pattern this follows.
 */
class ChatPollDelegate(
    private val conversationId: String,
    private val currentUserId: () -> String?,
    private val pollRepository: PollRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val updateState get() = context.updateState
    private val observedPollIds = mutableSetOf<String>()

    /**
     * Starts observing a poll's question/options/current-user-vote, keeping
     * [ChatState.pollUiStates] up to date. Called from PollBubble (via intent) the first time
     * a `poll:<id>` message is rendered — idempotent per pollId for the lifetime of this
     * delegate (i.e. of the owning ChatViewModel).
     */
    fun observePoll(pollId: String) {
        if (!observedPollIds.add(pollId)) return
        scope.launch {
            catchResult {
                pollRepository.observePollById(pollId).collect { poll ->
                    updateState { s ->
                        val current = s.pollUiStates[pollId] ?: PollUiState()
                        s.copy(pollUiStates = s.pollUiStates + (pollId to current.copy(poll = poll)))
                    }
                }
            }
        }
        scope.launch {
            catchResult {
                pollRepository.observeOptionsByPollId(pollId).collect { options ->
                    updateState { s ->
                        val current = s.pollUiStates[pollId] ?: PollUiState()
                        s.copy(pollUiStates = s.pollUiStates + (pollId to current.copy(options = options)))
                    }
                }
            }
        }
        val uid = currentUserId() ?: return
        scope.launch {
            catchResult {
                pollRepository.observeVotes(pollId, uid).collect { votes ->
                    updateState { s ->
                        val current = s.pollUiStates[pollId] ?: PollUiState()
                        s.copy(pollUiStates = s.pollUiStates + (pollId to current.copy(userVotes = votes)))
                    }
                }
            }
        }
    }

    fun createPoll(question: String, options: List<String>, allowMultiple: Boolean) {
        val userId = currentUserId() ?: return
        updateState { it.copy(showCreatePollSheet = false) }
        scope.launch {
            catchResult {
                val pollId = pollRepository.createPoll(
                    conversationId = conversationId,
                    question = question,
                    createdBy = userId,
                    options = options,
                    allowMultiple = allowMultiple,
                )
                sendMessageUseCase(conversationId, userId, "poll:$pollId")
            }.onFailure { e -> AppLogger.e(TAG, "createPoll failed", e); updateState { it.copy(error = "No se pudo crear la encuesta") } }
        }
    }

    fun votePoll(pollId: String, optionId: String) {
        val userId = currentUserId() ?: return
        scope.launch {
            catchResult { pollRepository.vote(pollId, userId, optionId) }
                .onFailure { e -> AppLogger.e(TAG, "votePoll failed", e); updateState { it.copy(error = "No se pudo registrar el voto") } }
        }
    }
}
