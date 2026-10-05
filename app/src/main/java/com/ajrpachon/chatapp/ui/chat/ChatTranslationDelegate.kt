package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.utils.AudioTranscriber
import com.ajrpachon.chatapp.utils.TranslationManager
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        if (messageId in getState().translation.translatingMessageIds) return
        updateState { it.copy(translation = it.translation.copy(translatingMessageIds = it.translation.translatingMessageIds + messageId)) }
        scope.launch {
            catchResult { translationManager.translate(text) }
                .onSuccess { translated ->
                    updateState {
                        it.copy(
                            translation = it.translation.copy(
                                translatedTexts = it.translation.translatedTexts + (messageId to translated),
                                translatingMessageIds = it.translation.translatingMessageIds - messageId,
                            ),
                        )
                    }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            translation = it.translation.copy(translatingMessageIds = it.translation.translatingMessageIds - messageId),
                            error = UiText.StringResource(R.string.chat_error_translate),
                        )
                    }
                }
        }
    }

    // KNOWN BUG, not fixed here: transcribeFromMic() records this device's live mic, not the
    // message's actual audio file — there is no UI call site today so it hasn't shipped
    // user-visibly. See docs/audio-transcription-todo.md for why (AudioTranscriber wraps
    // Android's SpeechRecognizer, which has no file-input API) and the recommended fix (an
    // Edge Function calling a cloud STT API that accepts the app's .m4a/AAC recordings, e.g.
    // OpenAI's Whisper transcription endpoint).
    fun transcribeAudio(messageId: String) {
        scope.launch {
            val transcript = catchResult { audioTranscriber.transcribeFromMic() }.getOrNull()
            val result = transcript?.let(UiText::Dynamic) ?: UiText.StringResource(R.string.chat_transcription_unavailable)
            updateState { s -> s.copy(translation = s.translation.copy(transcriptions = s.translation.transcriptions + (messageId to result))) }
        }
    }
}
