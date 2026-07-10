package com.ajrpachon.chatapp.ui.chat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import com.ajrpachon.chatapp.domain.repository.StickerPackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class StickerPackViewModel(private val repository: StickerPackRepository) : ViewModel() {
    val installedPacks: StateFlow<List<StickerPackBO>> = repository.getInstalledPacks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val availablePacks: StateFlow<List<StickerPackBO>> = repository.getAvailablePacks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun stickersForPack(packId: String): StateFlow<List<StickerBO>> = repository.getStickersForPack(packId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun installPack(packId: String) { viewModelScope.launch { repository.installPack(packId) } }
}