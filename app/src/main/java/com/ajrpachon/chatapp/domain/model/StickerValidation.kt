package com.ajrpachon.chatapp.domain.model

object StickerValidation {
    private const val MAX_STICKER_LENGTH = 10  // emoji sequences are short

    fun sanitize(stickerUrl: String?): String? =
        stickerUrl?.takeIf { it.isNotBlank() && it.length <= MAX_STICKER_LENGTH }
}
