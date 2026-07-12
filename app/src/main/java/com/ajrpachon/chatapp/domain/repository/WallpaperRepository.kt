package com.ajrpachon.chatapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    fun getWallpaperColor(conversationId: String): Flow<Long?>
    suspend fun setWallpaperColor(conversationId: String, color: Long?)
}
