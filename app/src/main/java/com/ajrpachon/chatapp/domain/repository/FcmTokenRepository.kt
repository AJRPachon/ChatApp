package com.ajrpachon.chatapp.domain.repository

interface FcmTokenRepository {
    fun savePendingToken(token: String)
    suspend fun syncToken(): Result<Unit>
    suspend fun deleteToken(): Result<Unit>
    suspend fun upsertToken(token: String)
}
