package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.domain.repository.MessageStatsRepository

class MessageStatsRepositoryImpl(
    private val messageDao: MessageDao,
) : MessageStatsRepository {
    override suspend fun countSent(userId: String): Int = messageDao.countSent(userId)
    override suspend fun countReceived(userId: String): Int = messageDao.countReceived(userId)
    override suspend fun countCalls(): Int = messageDao.countCalls()
    override suspend fun sumCallDurationSeconds(): Int = messageDao.sumCallDurationSeconds()
    override suspend fun countImages(): Int = messageDao.countImages()
    override suspend fun countAudio(): Int = messageDao.countAudio()
    override suspend fun countVideos(): Int = messageDao.countVideos()
    override suspend fun getMostActiveConversationId(): String? = messageDao.getMostActiveConversation()?.conversationId
    override suspend fun countMessagesByDay(since: Long): List<Pair<Long, Int>> =
        messageDao.countMessagesByDay(since).map { it.dayEpoch to it.count }
}
