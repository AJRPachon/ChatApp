package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationsUseCase(private val conversationRepository: ConversationRepository) {
    operator fun invoke(userId: String): Flow<List<ConversationBO>> =
        conversationRepository.observeConversations(userId)
}
