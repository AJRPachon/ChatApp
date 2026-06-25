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

@Serializable
internal data class GiphyResponse(
    @SerialName("data") val data: List<GiphyGif> = emptyList(),
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

internal suspend fun searchGiphy(query: String): List<GiphyGif> {
    val endpoint = if (query.isBlank()) {
        "https://api.giphy.com/v1/gifs/trending"
    } else {
        "https://api.giphy.com/v1/gifs/search"
    }
    return runCatching {
        giphyClient.get(endpoint) {
            parameter("api_key", BuildConfig.GIPHY_API_KEY)
            parameter("limit", 24)
            if (query.isNotBlank()) parameter("q", query)
            parameter("rating", "g")
        }.body<GiphyResponse>().data
    }.getOrElse { emptyList() }
}
