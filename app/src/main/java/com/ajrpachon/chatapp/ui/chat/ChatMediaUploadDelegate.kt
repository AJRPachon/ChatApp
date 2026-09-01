package com.ajrpachon.chatapp.ui.chat

import android.net.Uri
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.usecase.GetUriMetadataUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.UploadLimits
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ChatMediaUploadDelegate"
private const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
private const val DEFAULT_FILE_MIME_TYPE = "application/octet-stream"
private const val DEFAULT_FILE_DISPLAY_NAME = "archivo"

/**
 * Handles uploading and sending images (as a batch), a generic file, and a video. Part of the
 * eighth slice of docs/chat-viewmodel-decomposition.md — split out of the originally single
 * "ChatMediaDelegate" entry into three smaller delegates (this one, [ChatQuickSendDelegate],
 * [ChatAudioRecordingDelegate]) since one delegate covering all of it tripped detekt's
 * LongParameterList threshold, and the three really are distinct concerns anyway.
 */
class ChatMediaUploadDelegate(
    private val conversationId: String,
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getUriMetadataUseCase: GetUriMetadataUseCase,
    private val readUriAsBytesUseCase: ReadUriAsBytesUseCase,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState
    private val sendEffect get() = context.sendEffect

    fun sendImages(uris: List<Uri>) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(replyingTo = null) }
            val metadataByUri = uris.map { uri ->
                val metadata = catchResult {
                    withContext(Dispatchers.IO) { getUriMetadataUseCase(uri.toString()) }
                }.getOrNull()
                uri to metadata
            }
            val totalBytes = metadataByUri.sumOf { (_, metadata) -> metadata?.size?.takeIf { it >= 0 } ?: 0L }
            // Only smooth batches that would render as ImageGroupBubble (>2 images) — that's
            // where the per-message paging updates cause the reported bubble-shape jumping.
            // Single/double sends already render fine as each message lands.
            val showBatchPlaceholder = metadataByUri.size > 2
            updateState {
                it.copy(
                    mediaUpload = it.mediaUpload.copy(
                        progress = MediaUploadProgress(totalCount = metadataByUri.size, completedCount = 0, totalBytes = totalBytes),
                        pendingImageUris = if (showBatchPlaceholder) metadataByUri.map { (uri, _) -> uri } else emptyList(),
                        suppressedImageMessageIds = emptySet(),
                    ),
                )
            }
            for ((index, entry) in metadataByUri.withIndex()) {
                val (uri, metadata) = entry
                val bytes = catchResult {
                    readUriAsBytesUseCase(uri.toString())
                }.getOrNull()
                if (bytes != null) {
                    catchResult {
                        val mimeType = metadata?.mimeType ?: DEFAULT_IMAGE_MIME_TYPE
                        val imageUrl = messageRepository.uploadImage(conversationId, bytes, mimeType)
                        val replyForImage = if (index == 0) reply else null
                        sendMessageUseCase(
                            conversationId, userId, "", imageUrl,
                            replyToId = replyForImage?.id, replyToContent = replyForImage?.replySnippet(), replyToSenderName = replyForImage?.senderName,
                        ).getOrThrow()
                    }.onSuccess { message ->
                        if (showBatchPlaceholder) {
                            updateState {
                                it.copy(mediaUpload = it.mediaUpload.copy(suppressedImageMessageIds = it.mediaUpload.suppressedImageMessageIds + message.id))
                            }
                        }
                    }.onFailure { e -> AppLogger.e(TAG, "sendImages failed", e); updateState { it.copy(error = e.message ?: "Error uploading image") } }
                } else {
                    AppLogger.e(TAG, "sendImages: could not read bytes for $uri")
                    updateState { it.copy(error = "No se pudo leer la imagen") }
                }
                updateState { it.copy(mediaUpload = it.mediaUpload.copy(progress = it.mediaUpload.progress?.copy(completedCount = index + 1))) }
            }
            updateState {
                it.copy(mediaUpload = it.mediaUpload.copy(progress = null, pendingImageUris = emptyList(), suppressedImageMessageIds = emptySet()))
            }
        }
    }

    fun sendFile(uri: Uri) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(mediaUpload = it.mediaUpload.copy(isUploadingFile = true), replyingTo = null) }
            catchResult {
                val metadata = withContext(Dispatchers.IO) { getUriMetadataUseCase(uri.toString()) }
                val mimeType = metadata.mimeType ?: DEFAULT_FILE_MIME_TYPE
                val displayName = metadata.displayName ?: DEFAULT_FILE_DISPLAY_NAME
                val fileSize = metadata.size
                val bytes = readUriAsBytesUseCase(uri.toString())
                val fileUrl = messageRepository.uploadFile(conversationId, bytes, displayName, mimeType)
                sendMessageUseCase(
                    conversationId, userId, "", fileUrl = fileUrl, fileName = displayName,
                    fileSize = fileSize, fileMimeType = mimeType,
                    replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
                ).getOrThrow()
            }.onFailure { e -> AppLogger.e(TAG, "sendFile failed", e); updateState { it.copy(error = e.message ?: "Error al enviar el archivo") } }
            updateState { it.copy(mediaUpload = it.mediaUpload.copy(isUploadingFile = false)) }
        }
    }

    fun sendVideo(uri: Uri) {
        val userId = getState().currentUserId ?: return
        val reply = getState().replyingTo
        scope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(mediaUpload = it.mediaUpload.copy(isUploadingFile = true), replyingTo = null) }
            catchResult {
                val fileSize = withContext(Dispatchers.IO) { getUriMetadataUseCase(uri.toString()) }.size
                check(fileSize == null || fileSize <= UploadLimits.VIDEO_MAX_BYTES) {
                    "El video supera el tamaño máximo permitido (50 MB)"
                }
                val bytes = readUriAsBytesUseCase(uri.toString())
                val videoUrl = messageRepository.uploadVideo(conversationId, bytes)
                sendMessageUseCase(
                    conversationId, userId, "", videoUrl = videoUrl,
                    replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
                ).getOrThrow()
            }.onFailure { e -> AppLogger.e(TAG, "sendVideo failed", e); updateState { it.copy(error = e.message ?: "Error al enviar el video") } }
            updateState { it.copy(mediaUpload = it.mediaUpload.copy(isUploadingFile = false)) }
        }
    }
}
