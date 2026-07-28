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
        remoteSource.isUsernameAvailable(username)

    override suspend fun upsertProfile(user: UserBO) {
        userDao.upsert(user.toDBO())
    }

    override suspend fun updateLastSeen(userId: String) {
        remoteSource.updateLastSeen(userId)
        userDao.getById(userId)?.let { dbo ->
            userDao.upsert(dbo.copy(lastSeen = System.currentTimeMillis()))
        }
    }

    override suspend fun updateShowOnlineStatus(userId: String, show: Boolean) {
        remoteSource.updateShowOnlineStatus(userId, show)
        userDao.getById(userId)?.let { dbo ->
            userDao.upsert(dbo.copy(showOnlineStatus = show))
        }
    }

    override suspend fun updateDisplayName(userId: String, displayName: String) {
        remoteSource.updateDisplayName(userId, displayName)
        userDao.getById(userId)?.let { dbo ->
            userDao.upsert(dbo.copy(displayName = displayName))
        }
    }

    override fun observeUserById(id: String): Flow<UserBO?> =
        userDao.observeById(id).map { it?.toBO() }

    override suspend fun searchUsersByEmails(emails: List<String>): List<UserBO> =
        remoteSource.searchByEmails(emails).map { dto ->
            dto.toBO().also { bo -> com.ajrpachon.chatapp.utils.catchResult { userDao.upsert(bo.toDBO()) } }
        }

    override suspend fun clearCurrentUser() {
        userDao.clearCurrentUser()
    }

    override suspend fun markAsCurrentUser(userId: String, email: String): UserBO? {
        val dto = remoteSource.getProfileOrThrow(userId) ?: return null
        userDao.clearCurrentUser()
        userDao.upsert(dto.toDBO(email = email, isCurrentUser = true))
        return dto.toBO(email = email)
    }

    override suspend fun fetchProfileFromRemote(userId: String): UserBO? =
        remoteSource.getProfileOrThrow(userId)?.toBO()

    override suspend fun uploadAvatar(userId: String, bytes: ByteArray, mimeType: String): String {
        val url = remoteSource.uploadAvatar(userId, bytes, mimeType)
        userDao.getById(userId)?.let { userDao.upsert(it.copy(avatarUrl = url)) }
        return url
    }

    override suspend fun findUserByPhone(phone: String): UserBO? {
        val target = phone.normalizePhoneDigits()
        if (target.isBlank()) return null
        return remoteSource.getProfilesWithPhone()
            .firstOrNull { it.phone?.normalizePhoneDigits() == target }
            ?.toBO()
    }

    private fun String.normalizePhoneDigits(): String = filter { it.isDigit() }

    private fun UserBO.toDBO(isCurrentUser: Boolean = false) =
        com.ajrpachon.chatapp.data.local.entity.UserDBO(
            id = id,
            email = email,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            createdAt = createdAt.toEpochMilliseconds(),
            isCurrentUser = isCurrentUser,
            lastSeen = lastSeen?.toEpochMilliseconds(),
            showOnlineStatus = showOnlineStatus,
        )
}

