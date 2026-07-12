package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class GetSavedMessagesUseCase(private val messageRepository: MessageRepository) {
    /**
     * Returns a [Flow] of all messages saved by [currentUserId], in insertion order.
     * Each [MessageBO] already carries [MessageBO.senderName]; conversation-name enrichment
     * is the caller's responsibility via [ConversationRepository] or local cache.
     */
    operator fun invoke(currentUserId: String): Flow<List<MessageBO>> =
        messageRepository.getSavedMessages(currentUserId)
}
