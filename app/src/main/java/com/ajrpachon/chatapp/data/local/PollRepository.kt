package com.ajrpachon.chatapp.data.local

import com.ajrpachon.chatapp.data.local.dao.PollDao
import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.data.local.entity.PollVoteDBO
import kotlinx.coroutines.flow.Flow

class PollRepository(private val pollDao: PollDao) {

    suspend fun insertPoll(poll: PollDBO) = pollDao.insertPoll(poll)

    suspend fun insertOptions(options: List<PollOptionDBO>) = pollDao.insertOptions(options)

    fun observePollsByConversation(conversationId: String): Flow<List<PollDBO>> =
        pollDao.observePollsByConversation(conversationId)

    suspend fun getOptions(pollId: String): List<PollOptionDBO> = pollDao.getOptions(pollId)

    suspend fun getVote(pollId: String, userId: String): PollVoteDBO? =
        pollDao.getVote(pollId, userId)

    suspend fun vote(pollId: String, userId: String, optionId: String) =
        pollDao.vote(pollId, userId, optionId)

    fun observeVote(pollId: String, userId: String): Flow<PollVoteDBO?> =
        pollDao.observeVote(pollId, userId)

    fun observePollById(pollId: String): Flow<PollDBO?> =
        pollDao.observePollById(pollId)

    fun observeOptionsByPollId(pollId: String): Flow<List<PollOptionDBO>> =
        pollDao.observeOptionsByPollId(pollId)
}
