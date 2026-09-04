package com.ajrpachon.chatapp.domain.model

sealed interface GiphySearchResult {
    data class Success(val gifs: List<GiphyGif>) : GiphySearchResult
    data object ApiKeyInvalid : GiphySearchResult
    data object NetworkError : GiphySearchResult
}
