package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface ReactionDao {
    @Query("SELECT * FROM message_reactions WHERE messageId IN (SELECT id FROM messages WHERE conversationId = :conversationId)")
    fun observeByConversation(conversationId: String): Flow<List<ReactionDBO>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reaction: ReactionDBO)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND userId = :userId AND emoji = :emoji")
    suspend fun delete(messageId: String, userId: String, emoji: String)

    @Query("SELECT COUNT(*) FROM message_reactions WHERE messageId = :messageId AND userId = :userId AND emoji = :emoji")
    suspend fun exists(messageId: String, userId: String, emoji: String): Int

    @Query("DELETE FROM message_reactions WHERE messageId IN (SELECT id FROM messages WHERE conversationId = :conversationId)")
    suspend fun deleteByConversation(conversationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reactions: List<ReactionDBO>)
}
