package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.utils.OkHttpProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface GiphyResult {
    data class Success(val gifs: List<GiphyGif>) : GiphyResult
    data object ApiKeyInvalid : GiphyResult
    data object NetworkError : GiphyResult
}

@Serializable
internal data class GiphyResponse(
    @SerialName("data") val data: List<GiphyGif> = emptyList(),
    @SerialName("meta") val meta: GiphyMeta = GiphyMeta(),
)

@Serializable
internal data class GiphyMeta(
    @SerialName("status") val status: Int = 200,
    @SerialName("msg") val msg: String = "",
)

@Serializable
internal data class GiphyGif(
    @SerialName("images") val images: GiphyImages,
)

@Serializable
internal data class GiphyImages(
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyImageData,
    @SerialName("original") val original: GiphyImageData,
)

@Serializable
internal data class GiphyImageData(
    @SerialName("url") val url: String = "",
)

internal val giphyClient = HttpClient(OkHttp) {
    engine { preconfigured = OkHttpProvider.client }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

internal suspend fun searchGiphy(query: String): GiphyResult {
    if (BuildConfig.GIPHY_API_KEY.isBlank()) return GiphyResult.ApiKeyInvalid
    val endpoint = if (query.isBlank()) {
        "https://api.giphy.com/v1/gifs/trending"
    } else {
        "https://api.giphy.com/v1/gifs/search"
    }
    return runCatching {
        val response = giphyClient.get(endpoint) {
            parameter("api_key", BuildConfig.GIPHY_API_KEY)
            parameter("limit", 24)
            if (query.isNotBlank()) parameter("q", query)
            parameter("rating", "g")
        }.body<GiphyResponse>()
        when (response.meta.status) {
            200 -> GiphyResult.Success(response.data)
            401, 403 -> GiphyResult.ApiKeyInvalid
            else -> GiphyResult.NetworkError
        }
    }.getOrElse { GiphyResult.NetworkError }
}
