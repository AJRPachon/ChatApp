package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiphyResponseDTO(
    @SerialName("data") val data: List<GiphyGifDTO> = emptyList(),
    @SerialName("meta") val meta: GiphyMetaDTO = GiphyMetaDTO(),
)

@Serializable
data class GiphyMetaDTO(
    @SerialName("status") val status: Int = 200,
    @SerialName("msg") val msg: String = "",
)

@Serializable
data class GiphyGifDTO(
    @SerialName("images") val images: GiphyImagesDTO,
)

@Serializable
data class GiphyImagesDTO(
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyImageDataDTO,
    @SerialName("original") val original: GiphyImageDataDTO,
)

@Serializable
data class GiphyImageDataDTO(
    @SerialName("url") val url: String = "",
)
