package com.ajrpachon.chatapp.ui.chat

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ajrpachon.chatapp.domain.repository.DraftRepository
import com.ajrpachon.chatapp.domain.repository.ScheduledMessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.worker.ScheduledMessageWorker
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles scheduled messages: scheduling the current input for later delivery, and cancelling
 * an already-scheduled one. Fourth slice of the decomposition in
 * docs/chat-viewmodel-decomposition.md — see [ChatAiDelegate] for the pattern this follows.
 *
 * Draft-save debouncing itself stays owned by ChatViewModel (it's shared with plain typing and
 * sending, not scheduling-specific) — [cancelDraftSave] is the narrow hook this delegate needs
 * into that: cancel any pending debounced draft write before this delegate persists its own
 * "draft cleared" write, so the debounce doesn't race and resurrect the just-cleared draft.
 */
class ChatSchedulingDelegate(
    private val conversationId: String,
    private val scheduledMessageRepository: ScheduledMessageRepository,
    private val draftRepository: DraftRepository,
    private val workManager: WorkManager,
    private val cancelDraftSave: () -> Unit,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState
    private val sendEffect get() = context.sendEffect

    fun scheduleMessage(scheduledAt: Long) {
        val text = getState().inputText.trim()
        val userId = getState().currentUserId
        if (userId == null || text.isBlank()) {
            updateState { it.copy(scheduling = it.scheduling.copy(showDialog = false), error = "Escribe un mensaje antes de programarlo") }
            return
        }
        updateState { it.copy(scheduling = it.scheduling.copy(showDialog = false, scheduledAtMs = scheduledAt), inputText = "") }
        cancelDraftSave()
        scope.launch {
            draftRepository.saveDraft(conversationId, "")
            val msgId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            catchResult {
                scheduledMessageRepository.schedule(
                    id = msgId,
                    conversationId = conversationId,
                    senderId = userId,
                    text = text,
                    scheduledAtMs = scheduledAt,
                    createdAt = now,
                )
            }
                .onSuccess {
                    val delayMs = (scheduledAt - System.currentTimeMillis()).coerceAtLeast(0L)
                    workManager.enqueue(
                        OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                            .addTag(ScheduledMessageWorker.WORK_TAG).build()
                    )
                    sendEffect(ChatEffect.ShowSnackbar("Mensaje programado"))
                }
                .onFailure { updateState { it.copy(error = "No se pudo programar el mensaje", inputText = text) } }
        }
    }

    fun cancelScheduledMessage(id: String) {
        scope.launch {
            workManager.cancelAllWorkByTag("scheduled_$id")
            catchResult { scheduledMessageRepository.deleteById(id) }
        }
    }
}
