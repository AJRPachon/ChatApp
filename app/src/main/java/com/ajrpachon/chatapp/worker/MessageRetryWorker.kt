package com.ajrpachon.chatapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageRetryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val messageRepository: MessageRepository by inject()
    private val sendMessageUseCase: SendMessageUseCase by inject()

    override suspend fun doWork(): Result {
        val pending = messageRepository.getPendingMessages()
        AppLogger.d(TAG, "MessageRetryWorker: found ${pending.size} pending/failed messages")

        var anyFailed = false
        for (msg in pending) {
            sendMessageUseCase(
                conversationId = msg.conversationId,
                senderId = msg.senderId,
                content = msg.content,
                imageUrl = msg.imageUrl,
                audioUrl = msg.audioUrl,
                replyToId = msg.replyToId,
                replyToContent = msg.replyToContent,
                replyToSenderName = msg.replyToSenderName,
                gifUrl = msg.gifUrl,
                stickerUrl = msg.stickerUrl,
                fileUrl = msg.fileUrl,
                fileName = msg.fileName,
                fileSize = msg.fileSize,
                fileMimeType = msg.fileMimeType,
                videoUrl = msg.videoUrl,
            ).onSuccess {
                messageRepository.updateSendStatus(msg.id, "sent")
                AppLogger.d(TAG, "Retried and sent message ${msg.id}")
            }.onFailure { e ->
                messageRepository.updateSendStatus(msg.id, "failed")
                anyFailed = true
                AppLogger.e(TAG, "Retry failed for message ${msg.id}", e)
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        const val TAG = "MessageRetryWorker"
        const val WORK_NAME = "message_retry"
    }
}
