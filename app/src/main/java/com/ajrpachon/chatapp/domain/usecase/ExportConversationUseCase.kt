package com.ajrpachon.chatapp.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportConversationUseCase(
    private val messageRepository: MessageRepository,
    private val context: Context,
    private val fileProviderAuthority: String,
) {

    /**
     * Fetches all messages in [conversationId] visible to [currentUserId], formats them as
     * a human-readable text file and returns a content [Uri] via FileProvider for sharing.
     */
    suspend operator fun invoke(
        conversationId: String,
        currentUserId: String,
    ): Result<Uri> = catchResult {
        val messages = withContext(Dispatchers.IO) {
            messageRepository.getAllMessages(conversationId, currentUserId)
        }
        val text = formatMessages(messages)
        withContext(Dispatchers.IO) {
            val outFile = File(context.cacheDir, "chat_$conversationId.txt")
            FileOutputStream(outFile).use { it.write(text.toByteArray()) }
            FileProvider.getUriForFile(context, fileProviderAuthority, outFile)
        }
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
