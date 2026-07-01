package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.FolderConversationDBO
import com.ajrpachon.chatapp.data.local.entity.FolderDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<FolderDBO>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderDBO)

    @Delete
    suspend fun delete(folder: FolderDBO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addConversation(link: FolderConversationDBO)

    @Query("DELETE FROM folder_conversations WHERE folderId = :folderId AND conversationId = :conversationId")
    suspend fun removeConversation(folderId: String, conversationId: String)

    @Query("SELECT conversationId FROM folder_conversations WHERE folderId = :folderId")
    suspend fun getConversationIds(folderId: String): List<String>

    @Query("DELETE FROM folder_conversations WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String)
}
