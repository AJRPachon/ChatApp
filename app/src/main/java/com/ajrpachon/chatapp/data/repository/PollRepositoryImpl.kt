package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.domain.model.PollBO
import com.ajrpachon.chatapp.domain.model.PollOptionBO
import com.ajrpachon.chatapp.domain.model.PollVoteBO
import com.ajrpachon.chatapp.domain.repository.PollRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import com.ajrpachon.chatapp.data.local.PollRepository as PollLocalDataSource

class PollRepositoryImpl(
    private val localDataSource: PollLocalDataSource,
) : PollRepository {

    override suspend fun createPoll(
        conversationId: String,
        question: String,
        createdBy: String,
        options: List<String>,
    ): String {
        val pollId = UUID.randomUUID().toString()
        localDataSource.insertPoll(
            PollDBO(
                id = pollId,
                conversationId = conversationId,
                question = question,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
            )
        )
        localDataSource.insertOptions(
            options.mapIndexed { index, text ->
                PollOptionDBO(
                    id = "$pollId-$index",
                    pollId = pollId,
                    text = text,
                )
            }
        )
        return pollId
    }

    override suspend fun vote(pollId: String, userId: String, optionId: String) {
        localDataSource.vote(pollId, userId, optionId)
    }

    override fun observePollById(pollId: String): Flow<PollBO?> =
        localDataSource.observePollById(pollId).map { dbo ->
            dbo?.let { PollBO(it.id, it.conversationId, it.question, it.createdBy, it.createdAt) }
        }

    override fun observeOptionsByPollId(pollId: String): Flow<List<PollOptionBO>> =
        localDataSource.observeOptionsByPollId(pollId).map { list ->
            list.map { PollOptionBO(it.id, it.pollId, it.text, it.voteCount) }
        }

    override fun observeVote(pollId: String, userId: String): Flow<PollVoteBO?> =
        localDataSource.observeVote(pollId, userId).map { dbo ->
            dbo?.let { PollVoteBO(it.pollId, it.userId, it.optionId) }
        }
}
