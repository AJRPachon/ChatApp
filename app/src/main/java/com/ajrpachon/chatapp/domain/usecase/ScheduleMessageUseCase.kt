package com.ajrpachon.chatapp.domain.usecase

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ajrpachon.chatapp.domain.repository.ScheduledMessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.worker.ScheduledMessageWorker
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScheduleMessageUseCase(
    private val scheduledMessageRepository: ScheduledMessageRepository,
    private val workManager: WorkManager,
) {
    /**
     * Persists a scheduled message and enqueues a [ScheduledMessageWorker] to deliver it
     * at [scheduledAtMs] (epoch milliseconds).
     *
     * @return the generated message ID wrapped in a [Result].
     */
    suspend operator fun invoke(
        conversationId: String,
        senderId: String,
        content: String,
        scheduledAtMs: Long,
    ): Result<String> = catchResult {
        require(content.isNotBlank()) { "El mensaje no puede estar vacío" }
        require(scheduledAtMs > System.currentTimeMillis()) { "La hora programada debe ser futura" }

        val id = UUID.randomUUID().toString()
        scheduledMessageRepository.schedule(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            text = content,
            scheduledAtMs = scheduledAtMs,
            createdAt = System.currentTimeMillis(),
        )

        val delayMs = (scheduledAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(ScheduledMessageWorker.WORK_TAG)
            .build()
        workManager.enqueue(request)

        id
    }
}
