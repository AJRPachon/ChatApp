package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.GiphyGif

enum class GifPickerError {
    API_KEY_INVALID,
    NETWORK_ERROR,
}

data class GifPickerState(
    val query: String = "",
    val gifs: List<GiphyGif> = emptyList(),
    val isLoading: Boolean = true,
    val errorState: GifPickerError? = null,
    val showKeyDialog: Boolean = false,
    val savedApiKey: String = "",
)

sealed interface GifPickerIntent {
    data class QueryChanged(val query: String) : GifPickerIntent
    data class SaveApiKey(val key: String) : GifPickerIntent
    data object ShowKeyDialog : GifPickerIntent
    data object DismissKeyDialog : GifPickerIntent
}
