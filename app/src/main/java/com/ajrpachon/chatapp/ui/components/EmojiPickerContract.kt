package com.ajrpachon.chatapp.ui.components

import com.ajrpachon.chatapp.domain.model.EmojiCategory

data class EmojiPickerState(
    val categories: List<EmojiCategory> = emptyList(),
    val selectedTab: Int = 0,
)

sealed interface EmojiPickerIntent {
    data class SelectTab(val index: Int) : EmojiPickerIntent
    data class EmojiClicked(val emoji: String) : EmojiPickerIntent
}

sealed interface EmojiPickerEffect {
    data class EmojiChosen(val emoji: String) : EmojiPickerEffect
}
