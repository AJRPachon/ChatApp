package com.ajrpachon.chatapp.ui.chat

import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ChatAudioRecordingDelegate"
private const val AMPLITUDE_SAMPLE_INTERVAL_MS = 50L
private const val MAX_AMPLITUDE = 32767f

/**
 * Handles voice-message recording: start/stop/discard, sending the finished recording, and the
 * MediaRecorder resource lifecycle itself (including [cleanup] for the owning ChatViewModel's
 * `onCleared()`). Part of the eighth slice of docs/chat-viewmodel-decomposition.md — see
 * [ChatMediaUploadDelegate]'s doc for why this concern was split into 3 delegates instead of 1.
 *
 * Draft-save debouncing stays owned by ChatViewModel (shared with plain typing/sending/
 * scheduling) — [clearDraft] is the narrow hook this delegate needs into that: cancel any
 * pending debounced draft write and persist the now-cleared input, in one call (folded into a
 * single param, unlike [ChatSchedulingDelegate]'s separate draftRepository/cancelDraftSave,
 * since an 8th param here would trip detekt's LongParameterList threshold).
 */
class ChatAudioRecordingDelegate(
    private val conversationId: String,
    private val application: Application,
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val clearDraft: suspend () -> Unit,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState
    private val sendEffect get() = context.sendEffect

    private var recorder: MediaRecorder? = null
    private var recordingTimerJob: Job? = null

    fun startRecording() {
        val outputFilePath = File.createTempFile("audio_", ".m4a", application.cacheDir).absolutePath
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(application)
                  else @Suppress("DEPRECATION") MediaRecorder()
        catchResult {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)
                prepare(); start()
            }
            recorder = rec
            updateState { it.copy(audioState = AudioState(isRecording = true, pendingFilePath = outputFilePath)) }
            val startMs = System.currentTimeMillis()
            recordingTimerJob = scope.launch {
                while (true) {
                    delay(AMPLITUDE_SAMPLE_INTERVAL_MS)
                    val elapsed = System.currentTimeMillis() - startMs
                    val amp = catchResult { (recorder?.maxAmplitude ?: 0).toFloat() / MAX_AMPLITUDE }.getOrDefault(0f)
                    updateState { s ->
                        // Keep the full history (not just a recent window) so the post-recording
                        // preview waveform can reflect the whole recording, not just its tail.
                        val newHistory = s.audioState.amplitudeHistory + amp
                        s.copy(audioState = s.audioState.copy(recordingDurationMs = elapsed, amplitudeHistory = newHistory))
                    }
                }
            }
        }.onFailure { e ->
            AppLogger.e(TAG, "Recording failed", e)
            catchResult { rec.release() }
            updateState { it.copy(error = "No se pudo iniciar la grabacion") }
        }
    }

    fun stopRecording() {
        recordingTimerJob?.cancel(); recordingTimerJob = null
        val durationMs = getState().audioState.recordingDurationMs
        catchResult { recorder?.apply { stop(); release() } }; recorder = null
        updateState { it.copy(audioState = it.audioState.copy(isRecording = false, recordingDurationMs = durationMs)) }
    }

    fun discardAudio() {
        getState().audioState.pendingFilePath?.let { path ->
            catchResult { File(path).delete() }
        }
        updateState { it.copy(audioState = AudioState()) }
    }

    fun sendAudio() {
        val userId = getState().currentUserId ?: return
        val filePath = getState().audioState.pendingFilePath ?: return
        val durationMs = getState().audioState.recordingDurationMs
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(audioState = it.audioState.copy(isUploading = true), replyingTo = null) }
            clearDraft()
            catchResult {
                val bytes = withContext(Dispatchers.IO) { File(filePath).readBytes() }
                val audioUrl = messageRepository.uploadAudio(conversationId, bytes)
                sendMessageUseCase(
                    conversationId, userId, "", audioUrl = audioUrl, audioDurationMs = durationMs,
                    replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
                )
                catchResult { File(filePath).delete() }
                updateState { it.copy(audioState = AudioState()) }
            }.onFailure { e ->
                updateState { it.copy(audioState = it.audioState.copy(isUploading = false), error = e.message ?: "Error al enviar el audio") }
            }
        }
    }

    /** Called from ChatViewModel.onCleared() — stops the recorder and releases it if still active. */
    fun cleanup() {
        recordingTimerJob?.cancel()
        catchResult { recorder?.apply { stop(); release() } }
        recorder = null
    }
}
