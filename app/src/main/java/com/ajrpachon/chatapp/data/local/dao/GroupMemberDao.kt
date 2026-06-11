package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE conversationId = :conversationId")
    fun observeByConversation(conversationId: String): Flow<List<GroupMemberDBO>>

    @Upsert
    suspend fun upsertAll(members: List<GroupMemberDBO>)

    @Upsert
    suspend fun upsert(member: GroupMemberDBO)

    @Query("DELETE FROM group_members WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun delete(conversationId: String, userId: String)

    @Query("SELECT * FROM group_members WHERE conversationId = :conversationId")
    suspend fun getAllForConversation(conversationId: String): List<GroupMemberDBO>

    @Query("SELECT * FROM group_members WHERE conversationId = :conversationId AND userId = :userId LIMIT 1")
    suspend fun getByUser(conversationId: String, userId: String): GroupMemberDBO?

    @Query("SELECT role FROM group_members WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun getRole(conversationId: String, userId: String): String?

    @Query("DELETE FROM group_members WHERE conversationId = :conversationId")
    suspend fun deleteAllForConversation(conversationId: String)

    // Atomically replaces the full member list — no intermediate empty state visible to observers.
    @Transaction
    suspend fun replaceAll(conversationId: String, members: List<GroupMemberDBO>) {
        deleteAllForConversation(conversationId)
        if (members.isNotEmpty()) insertAll(members)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<GroupMemberDBO>)
}
