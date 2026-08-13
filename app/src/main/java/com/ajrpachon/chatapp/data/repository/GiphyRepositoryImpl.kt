package com.ajrpachon.chatapp.data.repository

import android.content.Context
import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.data.mapper.toDomain
import com.ajrpachon.chatapp.data.remote.source.GiphyRemoteSource
import com.ajrpachon.chatapp.data.session.AndroidSecureStorage
import com.ajrpachon.chatapp.domain.model.GiphySearchResult
import com.ajrpachon.chatapp.domain.repository.GiphyRepository

class GiphyRepositoryImpl(
    private val remoteSource: GiphyRemoteSource,
    context: Context,
) : GiphyRepository {

    private val storage by lazy { AndroidSecureStorage(context.applicationContext, PREFS_NAME) }

    override suspend fun search(query: String): GiphySearchResult {
        val apiKey = getApiKey()?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GIPHY_API_KEY.takeIf { it.isNotBlank() }
            ?: return GiphySearchResult.ApiKeyInvalid
        return runCatching {
            val response = remoteSource.search(apiKey, query)
            when (response.meta.status) {
                200 -> GiphySearchResult.Success(response.data.map { it.toDomain() })
                401, 403 -> GiphySearchResult.ApiKeyInvalid
                else -> GiphySearchResult.NetworkError
            }
        }.getOrElse { GiphySearchResult.NetworkError }
    }

    override fun getApiKey(): String? = storage.getString(KEY)

    override fun setApiKey(key: String) {
        storage.putString(KEY, key.trim())
    }

    companion object {
        private const val PREFS_NAME = "giphy_key_prefs"
        private const val KEY = "giphy_api_key"
    }
}
