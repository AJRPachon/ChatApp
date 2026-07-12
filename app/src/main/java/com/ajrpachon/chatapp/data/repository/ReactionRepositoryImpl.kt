package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.source.ReactionRemoteSource
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReactionRepositoryImpl(
    private val reactionDao: ReactionDao,
    private val remoteSource: ReactionRemoteSource,
) : ReactionRepository {

    override fun observeReactions(conversationId: String): Flow<Map<String, List<ReactionBO>>> =
        reactionDao.observeByConversation(conversationId).map { dbos ->
            dbos.map { it.toBO() }
                .groupBy { it.messageId }
        }

    override suspend fun toggleReaction(messageId: String, userId: String, emoji: String) {
        val exists = reactionDao.exists(messageId, userId, emoji) > 0
        if (exists) {
            reactionDao.delete(messageId, userId, emoji)
            runCatching { remoteSource.deleteReaction(messageId, userId, emoji) }
        } else {
            reactionDao.insert(ReactionDBO(messageId, userId, emoji))
            runCatching { remoteSource.insertReaction(messageId, userId, emoji) }
        }
    }
}
