package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.data.local.dao.StatusDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import com.ajrpachon.chatapp.data.remote.source.StatusRemoteSource
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
private const val STATUS_TTL_MS = 24 * 60 * 60 * 1000L // 24 h
private const val TAG = "StatusRepo"
class StatusRepositoryImpl(
    private val statusDao: StatusDao,
    private val userDao: UserDao,
    private val remoteSource: StatusRemoteSource,
    private val analyticsTracker: AnalyticsTracker,
) : StatusRepository {
    override fun observeActiveStatuses(contactIds: List<String>): Flow<List<StatusBO>> = channelFlow {
        // Realtime: someone in contactIds (or the current user) posts/edits/removes a status →
        // resync so the change (and any new sender profile) lands in Room. RLS already limits
        // which rows we're notified about to "own or shares-a-direct-conversation-with", so no
        // extra client-side filtering is needed here — syncStatuses re-applies the contactIds
        // filter on top regardless.
        launch {
            catchResult {
                val userId = remoteSource.getCurrentUserId() ?: return@catchResult
                remoteSource.observeStatusChanges(userId).collect {
                    catchResult { syncStatuses(contactIds) }
                }
            }.onFailure { e -> AppLogger.e(TAG, "Realtime status subscription failed", e) }
        }

        statusDao.observeActive(System.currentTimeMillis()).map { dbos ->
            dbos.mapNotNull { dbo ->
                val user = userDao.getById(dbo.userId) ?: return@mapNotNull null
                val currentUserId = remoteSource.getCurrentUserId()
                dbo.toBO(
                    userName = user.displayName,
                    userAvatarUrl = user.avatarUrl,
                    isFromMe = dbo.userId == currentUserId,
                )
            }
        }.collect { send(it) }
    }
    override suspend fun syncStatuses(contactIds: List<String>) {
        val currentUserId = remoteSource.getCurrentUserId() ?: return
        val allIds = (contactIds + currentUserId).distinct()
        catchResult {
            statusDao.deleteExpired(System.currentTimeMillis())
            val dtos = remoteSource.getActiveStatuses(allIds)
            statusDao.upsertAll(dtos.map { it.toDBO() })
        }
    }
    override suspend fun postTextStatus(text: String, backgroundColor: Long) {
        val userId = remoteSource.getCurrentUserId() ?: return
        val now = System.currentTimeMillis()
        val dto = StatusDTO(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            text = text,
            backgroundColor = backgroundColor,
            createdAt = Instant.fromEpochMilliseconds(now).toString(),
            expiresAt = Instant.fromEpochMilliseconds(now + STATUS_TTL_MS).toString(),
        )
        remoteSource.postStatus(dto)
        statusDao.upsert(dto.toDBO())
        analyticsTracker.logEvent(
            AnalyticsEvents.STATUS_POSTED,
            mapOf(AnalyticsEvents.PARAM_STATUS_TYPE to AnalyticsEvents.TYPE_TEXT),
        )
    }
    override suspend fun postImageStatus(imageBytes: ByteArray, text: String?) {
        val userId = remoteSource.getCurrentUserId() ?: return
        val imageUrl = remoteSource.uploadStatusImage(userId, imageBytes)
        val now = System.currentTimeMillis()
        val dto = StatusDTO(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            text = text,
            imageUrl = imageUrl,
            createdAt = Instant.fromEpochMilliseconds(now).toString(),
            expiresAt = Instant.fromEpochMilliseconds(now + STATUS_TTL_MS).toString(),
        )
        remoteSource.postStatus(dto)
        statusDao.upsert(dto.toDBO())
        analyticsTracker.logEvent(
            AnalyticsEvents.STATUS_POSTED,
            mapOf(AnalyticsEvents.PARAM_STATUS_TYPE to AnalyticsEvents.TYPE_IMAGE),
        )
    }
    override suspend fun postVideoStatus(videoBytes: ByteArray, text: String?) {
        val userId = remoteSource.getCurrentUserId() ?: return
        val videoUrl = remoteSource.uploadStatusVideo(userId, videoBytes)
        val now = System.currentTimeMillis()
        val dto = StatusDTO(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            text = text,
            videoUrl = videoUrl,
            createdAt = Instant.fromEpochMilliseconds(now).toString(),
            expiresAt = Instant.fromEpochMilliseconds(now + STATUS_TTL_MS).toString(),
        )
        remoteSource.postStatus(dto)
        statusDao.upsert(dto.toDBO())
        analyticsTracker.logEvent(
            AnalyticsEvents.STATUS_POSTED,
            mapOf(AnalyticsEvents.PARAM_STATUS_TYPE to AnalyticsEvents.TYPE_VIDEO),
        )
    }
    override suspend fun deleteStatus(statusId: String) {
        remoteSource.deleteStatus(statusId)
        catchResult { statusDao.deleteExpired(0L) }
    }
}
