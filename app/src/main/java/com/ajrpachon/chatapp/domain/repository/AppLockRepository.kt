package com.ajrpachon.chatapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    val isEnabled: Flow<Boolean>
    val backgroundedAt: Flow<Long>
    suspend fun enable()
    suspend fun disable()
    suspend fun recordBackgroundedAt(timestamp: Long)
}
