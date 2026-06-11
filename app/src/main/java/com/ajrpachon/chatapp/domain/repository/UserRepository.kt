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
}
