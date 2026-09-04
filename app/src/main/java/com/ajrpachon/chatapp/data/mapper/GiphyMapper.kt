package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.GiphyGifDTO
import com.ajrpachon.chatapp.domain.model.GiphyGif

fun GiphyGifDTO.toDomain() = GiphyGif(
    previewUrl = images.fixedHeightSmall.url,
    fullUrl = images.original.url,
)
