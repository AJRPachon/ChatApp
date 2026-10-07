package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO

fun StickerPackDBO.toBO() = StickerPackBO(
    id = id,
    name = name,
    coverUrl = coverUrl,
    isInstalled = isInstalled,
)

fun StickerDBO.toBO() = StickerBO(
    id = id,
    packId = packId,
    imageUrl = imageUrl,
    tags = tags,
)
