package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(private val messageRepository: MessageRepository) {
    operator fun invoke(conversationId: String): Flow<List<MessageBO>> =
        messageRepository.observeMessages(conversationId)
}
