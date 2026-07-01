package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface BroadcastListDao {

    // ── Broadcast Lists ─────────────────────────────────────────────────────

    @Query("SELECT * FROM broadcast_lists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BroadcastListDBO>>

    @Query("SELECT * FROM broadcast_lists ORDER BY createdAt DESC")
    suspend fun getAll(): List<BroadcastListDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: BroadcastListDBO)

    @Query("DELETE FROM broadcast_lists WHERE id = :listId")
    suspend fun delete(listId: String)

    // ── Members ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<BroadcastListMemberDBO>)

    @Query("DELETE FROM broadcast_list_members WHERE listId = :listId")
    suspend fun deleteMembers(listId: String)

    @Query(
        """
        SELECT u.* FROM users u
        INNER JOIN broadcast_list_members m ON u.id = m.userId
        WHERE m.listId = :listId
        """
    )
    suspend fun getMembersForList(listId: String): List<UserDBO>

    // ── Atomic helpers ───────────────────────────────────────────────────────

    @Transaction
    suspend fun insertWithMembers(list: BroadcastListDBO, members: List<BroadcastListMemberDBO>) {
        insert(list)
        if (members.isNotEmpty()) insertMembers(members)
    }

    @Transaction
    suspend fun deleteWithMembers(listId: String) {
        deleteMembers(listId)
        delete(listId)
    }
}
