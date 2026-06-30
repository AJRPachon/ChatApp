package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerPackDao {

    @Query("SELECT * FROM sticker_packs WHERE is_installed = 1 ORDER BY name ASC")
    fun getInstalledPacks(): Flow<List<StickerPackDBO>>

    @Query("SELECT * FROM sticker_packs WHERE is_installed = 0 ORDER BY name ASC")
    fun getAvailablePacks(): Flow<List<StickerPackDBO>>

    @Query("SELECT * FROM stickers WHERE pack_id = :packId ORDER BY id ASC")
    fun getStickersForPack(packId: String): Flow<List<StickerDBO>>

    @Query("UPDATE sticker_packs SET is_installed = 1 WHERE id = :packId")
    suspend fun installPack(packId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPacks(packs: List<StickerPackDBO>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStickers(stickers: List<StickerDBO>)
}
