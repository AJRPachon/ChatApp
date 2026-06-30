package com.ajrpachon.chatapp.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Utility that wraps Android's [SpeechRecognizer] to perform live-mic transcription.
 *
 * Note: [SpeechRecognizer] must be created and used on the main thread.
 * [transcribeFromMic] handles this internally via [Dispatchers.Main].
 */
class AudioTranscriber {

    /**
     * Starts a live SpeechRecognizer session and returns the best transcription result.
     * If recognition is unavailable or fails, returns "Transcripción no disponible".
     *
     * Must be called from any dispatcher — switches to Main internally.
     */
    suspend fun transcribeFromMic(context: Context): String = withContext(Dispatchers.Main) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return@withContext "Transcripción no disponible"
        }

        suspendCancellableCoroutine { cont ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.takeIf { it.isNotBlank() }
                        ?: "Transcripción no disponible"
                    recognizer.destroy()
                    if (cont.isActive) cont.resume(text)
                }

                override fun onError(error: Int) {
                    recognizer.destroy()
                    if (cont.isActive) cont.resume("Transcripción no disponible")
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            cont.invokeOnCancellation { recognizer.destroy() }
            recognizer.startListening(intent)
        }
    }
}
