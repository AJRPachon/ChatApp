package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.ConversationFileExporter
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ExportConversationUseCase(
    private val messageRepository: MessageRepository,
    private val conversationFileExporter: ConversationFileExporter,
) {

    /**
     * Fetches all messages in [conversationId] visible to [currentUserId], formats them as
     * a human-readable text file and returns a shareable content URI (as [String]).
     */
    suspend operator fun invoke(
        conversationId: String,
        currentUserId: String,
    ): Result<String> = catchResult {
        val messages = withContext(Dispatchers.IO) {
            messageRepository.getAllMessages(conversationId, currentUserId)
        }
        val text = formatMessages(messages)
        conversationFileExporter.writeAndShare("chat_$conversationId.txt", text)
    }

    private fun formatMessages(messages: List<MessageBO>): String {
        val timeZone = TimeZone.currentSystemDefault()
        return buildString {
            for (msg in messages) {
                if (msg.isDeleted) continue
                val local = msg.createdAt.toLocalDateTime(timeZone)
                val date = "%02d:%02d %02d/%02d/%04d".format(
                    local.hour, local.minute, local.dayOfMonth, local.monthNumber, local.year,
                )
                val line = when {
                    msg.content.isNotBlank() -> "[$date] ${msg.senderName}: ${msg.content}"
                    msg.imageUrl != null -> "[$date] ${msg.senderName}: [Imagen]"
                    msg.audioUrl != null -> "[$date] ${msg.senderName}: [Audio]"
                    msg.gifUrl != null -> "[$date] ${msg.senderName}: [GIF]"
                    msg.stickerUrl != null -> "[$date] ${msg.senderName}: [Sticker]"
                    msg.fileUrl != null -> "[$date] ${msg.senderName}: [Archivo: ${msg.fileName ?: ""}]"
                    msg.videoUrl != null -> "[$date] ${msg.senderName}: [Video]"
                    else -> continue
                }
                appendLine(line)
            }
        }
    }
}
