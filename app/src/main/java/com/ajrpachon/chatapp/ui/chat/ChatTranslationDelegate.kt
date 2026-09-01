package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.utils.AudioTranscriber
import com.ajrpachon.chatapp.utils.TranslationManager
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val NO_TRANSCRIPTION_AVAILABLE = "Transcripcion no disponible"

/**
 * Handles per-message translation and audio transcription — second slice of the decomposition
 * in docs/chat-viewmodel-decomposition.md. See [ChatAiDelegate] for the pattern this follows
 * (state mutated only via [ChatDelegateContext], [ChatState]/[ChatIntent]/ChatScreen unchanged).
 */
class ChatTranslationDelegate(
    private val translationManager: TranslationManager,
    private val audioTranscriber: AudioTranscriber,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState

    fun translateMessage(messageId: String, text: String) {
        if (messageId in getState().translatingMessageIds) return
        updateState { it.copy(translatingMessageIds = it.translatingMessageIds + messageId) }
        scope.launch {
            catchResult { translationManager.translate(text) }
                .onSuccess { translated ->
                    updateState {
                        it.copy(
                            translatedTexts = it.translatedTexts + (messageId to translated),
                            translatingMessageIds = it.translatingMessageIds - messageId,
                        )
                    }
                }
                .onFailure {
                    updateState { it.copy(translatingMessageIds = it.translatingMessageIds - messageId, error = "No se pudo traducir") }
                }
        }
    }

    fun transcribeAudio(messageId: String) {
        scope.launch {
            val result = catchResult { audioTranscriber.transcribeFromMic() }.getOrDefault(NO_TRANSCRIPTION_AVAILABLE)
            updateState { s -> s.copy(audioTranscriptions = s.audioTranscriptions + (messageId to result)) }
        }
    }
}
