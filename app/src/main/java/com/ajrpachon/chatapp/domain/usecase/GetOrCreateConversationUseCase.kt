package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository

class GetOrCreateConversationUseCase(private val conversationRepository: ConversationRepository) {
    suspend operator fun invoke(currentUserId: String, otherUserId: String): ConversationBO =
        conversationRepository.getOrCreateDirectConversation(currentUserId, otherUserId)
}
