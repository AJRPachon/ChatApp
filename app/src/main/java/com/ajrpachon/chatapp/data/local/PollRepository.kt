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

    suspend fun getVotes(pollId: String, userId: String): List<PollVoteDBO> =
        pollDao.getVotes(pollId, userId)

    suspend fun vote(pollId: String, userId: String, optionId: String) =
        pollDao.vote(pollId, userId, optionId)

    fun observeVotes(pollId: String, userId: String): Flow<List<PollVoteDBO>> =
        pollDao.observeVotes(pollId, userId)

    fun observePollById(pollId: String): Flow<PollDBO?> =
        pollDao.observePollById(pollId)

    fun observeOptionsByPollId(pollId: String): Flow<List<PollOptionDBO>> =
        pollDao.observeOptionsByPollId(pollId)
}
