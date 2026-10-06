package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.data.mapper.toDomain
import com.ajrpachon.chatapp.data.remote.source.GiphyRemoteSource
import com.ajrpachon.chatapp.domain.model.GiphySearchResult
import com.ajrpachon.chatapp.domain.repository.GiphyRepository
import com.ajrpachon.chatapp.utils.SecureStorage

class GiphyRepositoryImpl(
    private val remoteSource: GiphyRemoteSource,
    storageProvider: () -> SecureStorage,
    private val defaultApiKey: String = BuildConfig.GIPHY_API_KEY,
) : GiphyRepository {

    // Lazy: creating the Keystore-backed storage is not needed until a key is read or written.
    private val storage by lazy(storageProvider)

    override suspend fun search(query: String): GiphySearchResult {
        val apiKey = getApiKey()?.takeIf { it.isNotBlank() }
            ?: defaultApiKey.takeIf { it.isNotBlank() }
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
        const val PREFS_NAME = "giphy_key_prefs"
        private const val KEY = "giphy_api_key"
    }
}
