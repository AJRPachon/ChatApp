package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val supabase: SupabaseClient,
) : ConversationRepository {

    override fun observeConversations(userId: String): Flow<List<ConversationBO>> =
        conversationDao.observeAll().map { dbos ->
            dbos.mapNotNull { dbo ->
                val lastMsg = messageDao.getLastMessage(dbo.id)?.let { msgDbo ->
                    val sender = userDao.getById(msgDbo.senderId)?.toBO()
                    msgDbo.toBO(userId, sender?.displayName ?: msgDbo.senderId)
                }
                ConversationBO(
                    id = dbo.id,
                    name = dbo.name ?: "Chat",
                    isGroup = dbo.isGroup,
                    participants = emptyList(),
                    lastMessage = lastMsg,
                    unreadCount = dbo.unreadCount,
                    updatedAt = Instant.fromEpochMilliseconds(dbo.updatedAt),
                )
            }
        }

    override suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        otherUserId: String,
    ): ConversationBO {
        val existing = supabase.postgrest
            .rpc("get_or_create_direct_conversation", mapOf(
                "user_a" to currentUserId,
                "user_b" to otherUserId,
            ))
            .decodeSingle<ConversationDBO>()

        conversationDao.upsert(existing)
        val otherUser = userDao.getById(otherUserId)?.toBO()
        return ConversationBO(
            id = existing.id,
            name = otherUser?.displayName ?: "Chat",
            isGroup = false,
            participants = listOfNotNull(otherUser),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = Instant.fromEpochMilliseconds(existing.updatedAt),
        )
    }

    override suspend fun syncConversations(userId: String) {
        val rows = supabase.postgrest["conversation_participants"]
            .select(Columns.raw("conversation_id, conversations(*)")) {
                filter { eq("user_id", userId) }
            }
        // Store locally — simplified; full implementation maps join result
    }
}
