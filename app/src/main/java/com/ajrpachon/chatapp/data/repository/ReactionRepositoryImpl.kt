package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ReactionDTO(
    @SerialName("message_id") val messageId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("emoji") val emoji: String,
)

class ReactionRepositoryImpl(
    private val reactionDao: ReactionDao,
    private val supabase: SupabaseClient,
) : ReactionRepository {

    override fun observeReactions(conversationId: String): Flow<Map<String, List<ReactionBO>>> =
        reactionDao.observeByConversation(conversationId).map { dbos ->
            dbos.map { ReactionBO(it.messageId, it.userId, it.emoji) }
                .groupBy { it.messageId }
        }

    override suspend fun toggleReaction(messageId: String, userId: String, emoji: String) {
        val exists = reactionDao.exists(messageId, userId, emoji) > 0
        if (exists) {
            reactionDao.delete(messageId, userId, emoji)
            runCatching {
                supabase.postgrest["message_reactions"].delete {
                    filter {
                        eq("message_id", messageId)
                        eq("user_id", userId)
                        eq("emoji", emoji)
                    }
                }
            }
        } else {
            reactionDao.insert(ReactionDBO(messageId, userId, emoji))
            runCatching {
                supabase.postgrest["message_reactions"].insert(
                    ReactionDTO(messageId, userId, emoji)
                )
            }
        }
    }
}
