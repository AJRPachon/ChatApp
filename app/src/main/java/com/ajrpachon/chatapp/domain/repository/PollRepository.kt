package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.PollBO
import com.ajrpachon.chatapp.domain.model.PollOptionBO
import com.ajrpachon.chatapp.domain.model.PollVoteBO
import kotlinx.coroutines.flow.Flow

interface PollRepository {
    /**
     * Creates a poll with the given [question] and [options] inside [conversationId].
     * Returns the generated poll ID.
     */
    suspend fun createPoll(
        conversationId: String,
        question: String,
        createdBy: String,
        options: List<String>,
        allowMultiple: Boolean = false,
    ): String

    /** Records a vote by [userId] for [optionId] in [pollId]. */
    suspend fun vote(pollId: String, userId: String, optionId: String)

    fun observePollById(pollId: String): Flow<PollBO?>
    fun observeOptionsByPollId(pollId: String): Flow<List<PollOptionBO>>
    fun observeVotes(pollId: String, userId: String): Flow<List<PollVoteBO>>
}
