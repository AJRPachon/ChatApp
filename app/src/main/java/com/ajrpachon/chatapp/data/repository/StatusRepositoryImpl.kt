package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.StatusDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.StatusDBO
import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import com.ajrpachon.chatapp.data.remote.source.StatusRemoteSource
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.utils.catchResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

private const val STATUS_TTL_MS = 24 * 60 * 60 * 1000L // 24 h

class StatusRepositoryImpl(
    private val statusDao: StatusDao,
    private val userDao: UserDao,
    private val remoteSource: StatusRemoteSource,
    private val supabase: SupabaseClient,
) : StatusRepository {

    override fun observeActiveStatuses(): Flow<List<StatusBO>> =
        statusDao.observeActive(System.currentTimeMillis()).map { dbos ->
            dbos.mapNotNull { dbo ->
                val user = userDao.getById(dbo.userId) ?: return@mapNotNull null
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                StatusBO(
                    id = dbo.id,
                    userId = dbo.userId,
                    userName = user.displayName,
                    userAvatarUrl = user.avatarUrl,
                    text = dbo.text,
                    imageUrl = dbo.imageUrl,
                    backgroundColor = dbo.backgroundColor,
                    createdAt = Instant.fromEpochMilliseconds(dbo.createdAt),
                    expiresAt = Instant.fromEpochMilliseconds(dbo.expiresAt),
                    isFromMe = dbo.userId == currentUserId,
                )
            }
        }

    override suspend fun syncStatuses(contactIds: List<String>) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return
        val allIds = (contactIds + currentUserId).distinct()
        catchResult {
            statusDao.deleteExpired(System.currentTimeMillis())
            val dtos = remoteSource.getActiveStatuses(allIds)
            statusDao.upsertAll(dtos.map { it.toDBO() })
        }
    }

    override suspend fun postTextStatus(text: String, backgroundColor: Long) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
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
    }

    override suspend fun postImageStatus(imageBytes: ByteArray, text: String?) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
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
    }

    override suspend fun deleteStatus(statusId: String) {
        remoteSource.deleteStatus(statusId)
        catchResult { statusDao.deleteExpired(0L) }
    }

    private fun StatusDTO.toDBO() = StatusDBO(
        id = id,
        userId = userId,
        text = text,
        imageUrl = imageUrl,
        backgroundColor = backgroundColor,
        createdAt = runCatching { Instant.parse(createdAt).toEpochMilliseconds() }.getOrDefault(System.currentTimeMillis()),
        expiresAt = runCatching { Instant.parse(expiresAt).toEpochMilliseconds() }.getOrDefault(System.currentTimeMillis() + STATUS_TTL_MS),
    )
}
