package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val remoteSource: UserRemoteSource,
) : UserRepository {

    override fun getCurrentUserId(): String? = remoteSource.getCurrentUserId()

    override fun getCurrentUser(): Flow<UserBO?> =
        userDao.observeCurrentUser().map { it?.toBO() }

    override suspend fun getUserById(id: String): UserBO? =
        userDao.getById(id)?.toBO()
            ?: remoteSource.getProfile(id)?.toBO().also { bo ->
                bo?.let { userDao.upsert(it.toDBO()) }
            }

    override suspend fun searchByUsername(query: String): List<UserBO> =
        remoteSource.searchByUsername(query).map { dto ->
            dto.toBO().also { bo -> catchResult { userDao.upsert(bo.toDBO()) } }
        }

    override suspend fun setUsername(userId: String, username: String): Result<UserBO> =
        catchResult {
            val userDto = remoteSource.setUsername(userId, username)
            val userDbo = userDto.toDBO(isCurrentUser = true)
            userDao.upsert(userDbo)
            userDbo.toBO()
        }

    override suspend fun isUsernameAvailable(username: String): Boolean =
        catchResult { remoteSource.isUsernameAvailable(username) }.getOrElse { throw it }

    override suspend fun upsertProfile(user: UserBO) {
        userDao.upsert(user.toDBO())
    }

    private fun UserBO.toDBO(isCurrentUser: Boolean = false) =
        com.ajrpachon.chatapp.data.local.entity.UserDBO(
            id = id,
            email = email,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            createdAt = createdAt.toEpochMilliseconds(),
            isCurrentUser = isCurrentUser,
        )
}
