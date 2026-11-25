package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.dto.ReactionChangeDTO
import com.ajrpachon.chatapp.data.remote.source.ReactionRemoteSource
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import com.ajrpachon.chatapp.domain.util.catchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReactionRepositoryImpl(
    private val reactionDao: ReactionDao,
    private val remoteSource: ReactionRemoteSource,
) : ReactionRepository {

    // While the chat observes its reactions, the other participants' reactions arrive through
    // Realtime and are written to Room, which this flow emits from (offline-first).
    override fun observeReactions(conversationId: String): Flow<Map<String, List<ReactionBO>>> = channelFlow {
        launch {
            catchResult {
                remoteSource.observeReactionChanges().collect { change ->
                    catchResult { applyRemoteChange(change) }
                }
            }
        }
        reactionDao.observeByConversation(conversationId)
            .map { dbos -> dbos.map { it.toBO() }.groupBy { it.messageId } }
            .collect { send(it) }
    }

    private suspend fun applyRemoteChange(change: ReactionChangeDTO) {
        val r = change.reaction
        if (change.removed) {
            reactionDao.delete(r.messageId, r.userId, r.emoji)
        } else {
            reactionDao.insert(ReactionDBO(r.messageId, r.userId, r.emoji))
        }
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
