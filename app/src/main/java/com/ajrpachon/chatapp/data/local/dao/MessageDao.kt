package com.ajrpachon.chatapp.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND createdAt >= :since ORDER BY createdAt ASC")
    fun observeByConversation(conversationId: String, since: Long): Flow<List<MessageDBO>>

    // ORDER BY DESC so newest messages are at index 0; LazyColumn uses reverseLayout=true.
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND createdAt >= :since ORDER BY createdAt DESC")
    fun getMessagesPaged(conversationId: String, since: Long): PagingSource<Int, MessageDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageDBO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageDBO>)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId")
    suspend fun markAllRead(conversationId: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun markDeleted(messageId: String)

    @Query("UPDATE messages SET content = :content, isEdited = 1, editedAt = :editedAt WHERE id = :messageId")
    suspend fun updateContent(messageId: String, content: String, editedAt: Long)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: String): MessageDBO?

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND createdAt < :threshold")
    suspend fun deleteMessagesBefore(conversationId: String, threshold: Long)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchMessages(conversationId: String, query: String): List<MessageDBO>

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE conversationId = :conversationId
          AND imageUrl IS NOT NULL
          AND createdAt > COALESCE(
            (SELECT MAX(createdAt) FROM messages
             WHERE conversationId = :conversationId AND imageUrl IS NULL),
            0)
    """)
    suspend fun getTrailingImageCount(conversationId: String): Int
}
