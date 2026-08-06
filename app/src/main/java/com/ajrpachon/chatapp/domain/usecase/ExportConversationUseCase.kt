package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.ConversationFileExporter
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val formatter = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        return buildString {
            for (msg in messages) {
                if (msg.isDeleted) continue
                val date = formatter.format(Date(msg.createdAt.toEpochMilliseconds()))
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
