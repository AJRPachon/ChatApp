package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.domain.repository.PollRepository
import java.util.UUID
import com.ajrpachon.chatapp.data.local.PollRepository as PollLocalRepository

class PollRepositoryImpl(
    private val pollLocalRepository: PollLocalRepository,
) : PollRepository {

    override suspend fun createPoll(
        conversationId: String,
        question: String,
        createdBy: String,
        options: List<String>,
    ): String {
        val pollId = UUID.randomUUID().toString()
        pollLocalRepository.insertPoll(
            PollDBO(
                id = pollId,
                conversationId = conversationId,
                question = question,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
            )
        )
        pollLocalRepository.insertOptions(
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
        pollLocalRepository.vote(pollId, userId, optionId)
    }
}
