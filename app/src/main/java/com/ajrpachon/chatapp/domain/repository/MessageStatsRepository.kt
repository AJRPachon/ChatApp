package com.ajrpachon.chatapp.domain.repository

/**
 * Aggregate message/media statistics, split out of [MessageRepository] — a distinct concern from
 * message CRUD (only `UsageStatsViewModel` needs this slice, and shouldn't have to depend on the
 * other 27 message-lifecycle methods to get a handful of counts).
 */
interface MessageStatsRepository {
    suspend fun countSent(userId: String): Int
    suspend fun countReceived(userId: String): Int
    suspend fun countCalls(): Int
    suspend fun sumCallDurationSeconds(): Int
    suspend fun countImages(): Int
    suspend fun countAudio(): Int
    suspend fun countVideos(): Int
    suspend fun getMostActiveConversationId(): String?
    suspend fun countMessagesByDay(since: Long): List<Pair<Long, Int>>
}
