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

    @Query("UPDATE messages SET expiresAt = :expiresAt WHERE id = :messageId")
    suspend fun setExpiry(messageId: String, expiresAt: Long?)

    @Query("DELETE FROM messages WHERE expiresAt IS NOT NULL AND expiresAt <= :now")
    suspend fun deleteExpired(now: Long)

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

    @Query("SELECT * FROM messages WHERE isSaved = 1 ORDER BY createdAt DESC")
    fun getSavedMessages(): Flow<List<MessageDBO>>

    @Query("UPDATE messages SET isSaved = :saved WHERE id = :messageId")
    suspend fun setSaved(messageId: String, saved: Boolean)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isPinned = 1 ORDER BY createdAt DESC")
    fun getPinnedMessages(conversationId: String): Flow<List<MessageDBO>>

    @Query("UPDATE messages SET isPinned = :pinned WHERE id = :messageId")
    suspend fun setPinned(messageId: String, pinned: Boolean)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getAllMessages(conversationId: String): List<MessageDBO>

    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    suspend fun getAllMessages(): List<MessageDBO>

    // ── Usage stats ──────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM messages WHERE senderId = :userId AND isDeleted = 0")
    suspend fun countSent(userId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE senderId != :userId AND isDeleted = 0")
    suspend fun countReceived(userId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE callType IS NOT NULL AND isDeleted = 0")
    suspend fun countCalls(): Int

    @Query("SELECT COALESCE(SUM(callDuration), 0) FROM messages WHERE callType IS NOT NULL AND callDuration IS NOT NULL AND isDeleted = 0")
    suspend fun sumCallDurationSeconds(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE imageUrl IS NOT NULL AND isDeleted = 0")
    suspend fun countImages(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE audioUrl IS NOT NULL AND isDeleted = 0")
    suspend fun countAudio(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE videoUrl IS NOT NULL AND isDeleted = 0")
    suspend fun countVideos(): Int

    @Query("SELECT conversationId, COUNT(*) as count FROM messages WHERE isDeleted = 0 GROUP BY conversationId ORDER BY count DESC LIMIT 1")
    suspend fun getMostActiveConversation(): ConversationMessageCount?

    @Query("SELECT (createdAt / 86400000) as dayEpoch, COUNT(*) as count FROM messages WHERE createdAt >= :since AND isDeleted = 0 GROUP BY dayEpoch ORDER BY dayEpoch ASC")
    suspend fun countMessagesByDay(since: Long): List<DayMessageCount>
}
