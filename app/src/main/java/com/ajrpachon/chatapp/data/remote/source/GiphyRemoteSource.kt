package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.GiphyResponseDTO
import com.ajrpachon.chatapp.utils.OkHttpProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.Closeable

/**
 * Owns the shared Ktor [HttpClient] used for Giphy API calls. This is a process-wide
 * singleton (managed by Koin) reused across every GIF search so the underlying OkHttp
 * connection pool is shared — it must NOT be recreated per request. Call [close] only
 * on app teardown.
 */
class GiphyRemoteSource : Closeable {

    private val client: HttpClient = HttpClient(OkHttp) {
        engine { preconfigured = OkHttpProvider.client }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun search(apiKey: String, query: String): GiphyResponseDTO {
        val endpoint = if (query.isBlank()) TRENDING_ENDPOINT else SEARCH_ENDPOINT
        return client.get(endpoint) {
            parameter("api_key", apiKey)
            parameter("limit", SEARCH_LIMIT)
            if (query.isNotBlank()) parameter("q", query)
            parameter("rating", "g")
        }.body()
    }

    override fun close() {
        client.close()
    }

    companion object {
        private const val TRENDING_ENDPOINT = "https://api.giphy.com/v1/gifs/trending"
        private const val SEARCH_ENDPOINT = "https://api.giphy.com/v1/gifs/search"
        private const val SEARCH_LIMIT = 24
    }
}
