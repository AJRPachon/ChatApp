package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.data.local.entity.PollVoteDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: PollDBO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<PollOptionDBO>)

    @Query("SELECT * FROM polls WHERE conversationId = :conversationId ORDER BY createdAt DESC")
    fun observePollsByConversation(conversationId: String): Flow<List<PollDBO>>

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId")
    suspend fun getOptions(pollId: String): List<PollOptionDBO>

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND userId = :userId LIMIT 1")
    suspend fun getVote(pollId: String, userId: String): PollVoteDBO?

    @Transaction
    suspend fun vote(pollId: String, userId: String, optionId: String) {
        val existing = getVote(pollId, userId)
        if (existing != null) return // already voted — no double vote
        incrementVoteCount(optionId)
        insertVote(PollVoteDBO(pollId = pollId, userId = userId, optionId = optionId))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: PollVoteDBO)

    @Query("UPDATE poll_options SET voteCount = voteCount + 1 WHERE id = :optionId")
    suspend fun incrementVoteCount(optionId: String)

    @Query("SELECT * FROM poll_options WHERE pollId IN (SELECT id FROM polls WHERE conversationId = :conversationId)")
    fun observeOptionsByConversation(conversationId: String): Flow<List<PollOptionDBO>>

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND userId = :userId LIMIT 1")
    fun observeVote(pollId: String, userId: String): Flow<PollVoteDBO?>

    @Query("SELECT * FROM polls WHERE id = :pollId LIMIT 1")
    fun observePollById(pollId: String): Flow<PollDBO?>

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId")
    fun observeOptionsByPollId(pollId: String): Flow<List<PollOptionDBO>>
}
