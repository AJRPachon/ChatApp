package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ReactionBO
import kotlinx.coroutines.flow.Flow

interface ReactionRepository {
    fun observeReactions(conversationId: String): Flow<Map<String, List<ReactionBO>>>
    suspend fun toggleReaction(messageId: String, userId: String, emoji: String)
}
