package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO

data class StickerPackState(
    val installedPacks: List<StickerPackBO> = emptyList(),
    val availablePacks: List<StickerPackBO> = emptyList(),
    val selectedPackId: String? = null,
    val stickersForSelectedPack: List<StickerBO> = emptyList(),
)

sealed interface StickerPackIntent {
    data class SelectPack(val packId: String) : StickerPackIntent
    data class InstallPack(val packId: String) : StickerPackIntent
}
