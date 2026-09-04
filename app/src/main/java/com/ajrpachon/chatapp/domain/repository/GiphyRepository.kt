package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.GiphySearchResult

interface GiphyRepository {
    suspend fun search(query: String): GiphySearchResult
    fun getApiKey(): String?
    fun setApiKey(key: String)
}
