package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.UserBO
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUserId(): String?
    fun getCurrentUser(): Flow<UserBO?>
    suspend fun getUserById(id: String): UserBO?
    suspend fun searchByUsername(query: String): List<UserBO>
    suspend fun setUsername(userId: String, username: String): Result<UserBO>
    suspend fun isUsernameAvailable(username: String): Boolean
    suspend fun upsertProfile(user: UserBO)
    suspend fun updateLastSeen(userId: String)
    suspend fun updateShowOnlineStatus(userId: String, show: Boolean)
    suspend fun updateDisplayName(userId: String, displayName: String)
    fun observeUserById(id: String): Flow<UserBO?>
    suspend fun searchUsersByEmails(emails: List<String>): List<UserBO>
    suspend fun clearCurrentUser()
    suspend fun markAsCurrentUser(userId: String, email: String): UserBO?
    suspend fun fetchProfileFromRemote(userId: String): UserBO?
    suspend fun uploadAvatar(userId: String, bytes: ByteArray, mimeType: String): String
    suspend fun findUserByPhone(phone: String): UserBO?
}
