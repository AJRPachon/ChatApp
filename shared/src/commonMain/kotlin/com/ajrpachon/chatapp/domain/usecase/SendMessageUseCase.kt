package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository

class SendMessageUseCase(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(
        conversationId: String,
        senderId: String,
        content: String,
    ): Result<MessageBO> = runCatching {
        require(content.isNotBlank()) { "Message cannot be blank" }
        messageRepository.sendMessage(conversationId, senderId, content.trim())
    }
}
