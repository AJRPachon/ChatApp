package com.ajrpachon.chatapp.ui.chat

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.repository.StickerPackRepository
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StickerPackViewModel(
    private val repository: StickerPackRepository,
) : BaseViewModel<StickerPackState, Nothing>(StickerPackState()) {

    init {
        viewModelScope.launch {
            repository.getInstalledPacks().collect { packs ->
                updateState { it.copy(installedPacks = packs) }
            }
        }
        viewModelScope.launch {
            repository.getAvailablePacks().collect { packs ->
                updateState { it.copy(availablePacks = packs) }
            }
        }
    }

    /** Returns a per-pack StateFlow used by composables needing stickers for multiple packs. */
    fun stickersForPack(packId: String): StateFlow<List<StickerBO>> =
        repository.getStickersForPack(packId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onIntent(intent: StickerPackIntent) {
        when (intent) {
            is StickerPackIntent.SelectPack -> updateState { it.copy(selectedPackId = intent.packId) }
            is StickerPackIntent.InstallPack -> viewModelScope.launch { repository.installPack(intent.packId) }
        }
    }
}
