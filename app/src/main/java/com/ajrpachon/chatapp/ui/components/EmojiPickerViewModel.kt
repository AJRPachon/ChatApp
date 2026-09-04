package com.ajrpachon.chatapp.ui.components

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.EmojiRepository
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.launch

class EmojiPickerViewModel(
    private val emojiRepository: EmojiRepository,
) : BaseViewModel<EmojiPickerState, EmojiPickerEffect>(EmojiPickerState()) {

    init {
        loadCategories()
    }

    fun onIntent(intent: EmojiPickerIntent) {
        when (intent) {
            is EmojiPickerIntent.SelectTab -> updateState { it.copy(selectedTab = intent.index) }
            is EmojiPickerIntent.EmojiClicked -> {
                emojiRepository.recordUsed(intent.emoji)
                sendEffect(EmojiPickerEffect.EmojiChosen(intent.emoji))
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val loaded = emojiRepository.getCategories().toMutableList()
            val recent = emojiRepository.getRecent()
            if (recent.isNotEmpty()) {
                loaded[0] = loaded[0].copy(emojis = recent)
            } else {
                loaded.removeAt(0)
            }
            updateState { it.copy(categories = loaded) }
        }
    }
}
