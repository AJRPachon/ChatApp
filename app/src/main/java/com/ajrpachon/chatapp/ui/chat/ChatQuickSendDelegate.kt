package com.ajrpachon.chatapp.ui.chat

import android.annotation.SuppressLint
import android.app.Application
import android.location.LocationManager
import android.net.Uri
import com.ajrpachon.chatapp.domain.model.LocationMessageFormat
import com.ajrpachon.chatapp.domain.repository.ContactRepository
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "ChatQuickSendDelegate"

/**
 * Handles the "one message, no upload progress" sends: GIFs, stickers, a shared contact card
 * (including resolving it from a picked device contact), and the current device location. Part
 * of the eighth slice of docs/chat-viewmodel-decomposition.md — see [ChatMediaUploadDelegate]'s
 * doc for why this concern was split into 3 delegates instead of 1.
 */
class ChatQuickSendDelegate(
    private val conversationId: String,
    private val application: Application,
    private val sendMessageUseCase: SendMessageUseCase,
    private val contactRepository: ContactRepository,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState
    private val sendEffect get() = context.sendEffect

    fun sendGif(url: String) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(showStickerPicker = false, replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, "", gifUrl = url,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> updateState { it.copy(error = e.message ?: "Error al enviar el GIF") } }
        }
    }

    fun sendSticker(emoji: String) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(showStickerPicker = false, replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, "", stickerUrl = emoji,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> updateState { it.copy(error = e.message ?: "Error al enviar el sticker") } }
        }
    }

    fun sendContact(name: String, phone: String) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        val content = "contact:{\"name\":${JSONObject.quote(name)},\"phone\":${JSONObject.quote(phone)}}"
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, content,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> AppLogger.e(TAG, "sendContact failed", e); updateState { it.copy(error = e.message ?: "Error al enviar el contacto") } }
        }
    }

    fun handleContactSelected(uri: Uri) {
        scope.launch {
            catchResult {
                val contact = contactRepository.getContactByUri(uri.toString())
                if (contact != null) {
                    sendContact(contact.name, contact.phoneNumber)
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "handleContactSelected failed", e)
                updateState { it.copy(error = "No se pudo leer el contacto") }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchAndSendLocation() {
        val lm = application.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.firstNotNullOfOrNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }
        if (location != null) {
            val url = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            sendLocationMessage(url)
        } else {
            scope.launch { sendEffect(ChatEffect.ShowSnackbar("No se pudo obtener la ubicacion")) }
        }
    }

    fun sendLocationMessage(mapsUrl: String) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, LocationMessageFormat.format(mapsUrl),
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> updateState { it.copy(error = e.message ?: "Error") } }
        }
    }
}
