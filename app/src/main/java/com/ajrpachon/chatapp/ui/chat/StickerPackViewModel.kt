package com.ajrpachon.chatapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.StickerPackDao
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StickerPackViewModel(
    private val dao: StickerPackDao,
) : ViewModel() {

    val installedPacks: StateFlow<List<StickerPackDBO>> = dao
        .getInstalledPacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availablePacks: StateFlow<List<StickerPackDBO>> = dao
        .getAvailablePacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun stickersForPack(packId: String): StateFlow<List<StickerDBO>> =
        dao.getStickersForPack(packId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun installPack(packId: String) {
        viewModelScope.launch { dao.installPack(packId) }
    }
}
