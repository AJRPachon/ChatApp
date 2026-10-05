package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.GiphyGifDTO
import com.ajrpachon.chatapp.domain.model.GiphyGifBO

fun GiphyGifDTO.toDomain() = GiphyGifBO(
    previewUrl = images.fixedHeightSmall.url,
    fullUrl = images.original.url,
)
