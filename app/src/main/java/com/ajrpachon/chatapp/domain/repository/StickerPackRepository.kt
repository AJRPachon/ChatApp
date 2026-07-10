package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import kotlinx.coroutines.flow.Flow

interface StickerPackRepository {
    fun getInstalledPacks(): Flow<List<StickerPackBO>>
    fun getAvailablePacks(): Flow<List<StickerPackBO>>
    fun getStickersForPack(packId: String): Flow<List<StickerBO>>
    suspend fun installPack(packId: String)
}
