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

    @Query("SELECT * FROM polls WHERE id = :pollId LIMIT 1")
    suspend fun getPoll(pollId: String): PollDBO?

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND userId = :userId")
    suspend fun getVotes(pollId: String, userId: String): List<PollVoteDBO>

    @Transaction
    suspend fun vote(pollId: String, userId: String, optionId: String) {
        val poll = getPoll(pollId) ?: return
        val existingVotes = getVotes(pollId, userId)
        val existingForOption = existingVotes.find { it.optionId == optionId }

        if (poll.allowMultiple) {
            // Each option toggles independently, without touching the user's other selections.
            if (existingForOption != null) {
                deleteVote(pollId, userId, optionId)
                decrementVoteCount(optionId)
            } else {
                insertVote(PollVoteDBO(pollId = pollId, userId = userId, optionId = optionId))
                incrementVoteCount(optionId)
            }
            return
        }

        // Single-choice: tapping the already-selected option un-votes it, tapping a
        // different one moves the vote (old option decremented, new one incremented).
        if (existingForOption != null) {
            deleteVote(pollId, userId, optionId)
            decrementVoteCount(optionId)
            return
        }
        existingVotes.forEach { old ->
            deleteVote(pollId, userId, old.optionId)
            decrementVoteCount(old.optionId)
        }
        insertVote(PollVoteDBO(pollId = pollId, userId = userId, optionId = optionId))
        incrementVoteCount(optionId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: PollVoteDBO)

    @Query("DELETE FROM poll_votes WHERE pollId = :pollId AND userId = :userId AND optionId = :optionId")
    suspend fun deleteVote(pollId: String, userId: String, optionId: String)

    @Query("UPDATE poll_options SET voteCount = voteCount + 1 WHERE id = :optionId")
    suspend fun incrementVoteCount(optionId: String)

    @Query("UPDATE poll_options SET voteCount = voteCount - 1 WHERE id = :optionId AND voteCount > 0")
    suspend fun decrementVoteCount(optionId: String)

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND userId = :userId")
    fun observeVotes(pollId: String, userId: String): Flow<List<PollVoteDBO>>

    @Query("SELECT * FROM polls WHERE id = :pollId LIMIT 1")
    fun observePollById(pollId: String): Flow<PollDBO?>

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId")
    fun observeOptionsByPollId(pollId: String): Flow<List<PollOptionDBO>>
}
