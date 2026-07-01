package com.ajrpachon.chatapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ajrpachon.chatapp.data.local.dao.ScheduledMessageDao
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScheduledMessageWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val scheduledMessageDao: ScheduledMessageDao by inject()
    private val sendMessageUseCase: SendMessageUseCase by inject()

    override suspend fun doWork(): Result {
        val nowMs = System.currentTimeMillis()
        val pending = scheduledMessageDao.getPending(nowMs)
        AppLogger.d(TAG, "ScheduledMessageWorker: found ${pending.size} pending messages")
        for (msg in pending) {
            sendMessageUseCase(
                conversationId = msg.conversationId,
                senderId = msg.senderId,
                content = msg.text,
            ).onSuccess {
                scheduledMessageDao.deleteById(msg.id)
                AppLogger.d(TAG, "Sent and deleted scheduled message ${msg.id}")
            }.onFailure { e ->
                AppLogger.e(TAG, "Failed to send scheduled message ${msg.id}", e)
            }
        }
        return Result.success()
    }

    companion object {
        const val TAG = "ScheduledMessageWorker"
        const val WORK_TAG = "scheduled_message"
    }
}
