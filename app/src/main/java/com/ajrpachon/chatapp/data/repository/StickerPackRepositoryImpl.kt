package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.StickerPackDao
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.StickerPackRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StickerPackRepositoryImpl(
    private val dao: StickerPackDao,
    private val analyticsTracker: AnalyticsTracker,
) : StickerPackRepository {

    override fun getInstalledPacks(): Flow<List<StickerPackBO>> =
        dao.getInstalledPacks().map { list -> list.map { it.toBO() } }

    override fun getAvailablePacks(): Flow<List<StickerPackBO>> =
        dao.getAvailablePacks().map { list -> list.map { it.toBO() } }

    override fun getStickersForPack(packId: String): Flow<List<StickerBO>> =
        dao.getStickersForPack(packId).map { list -> list.map { it.toBO() } }

    override suspend fun installPack(packId: String) {
        dao.installPack(packId)
        analyticsTracker.logEvent(AnalyticsEvents.STICKER_PACK_INSTALLED)
    }
}

private fun StickerPackDBO.toBO() = StickerPackBO(
    id = id,
    name = name,
    coverUrl = coverUrl,
    isInstalled = isInstalled,
)

private fun StickerDBO.toBO() = StickerBO(
    id = id,
    packId = packId,
    imageUrl = imageUrl,
    tags = tags,
)
