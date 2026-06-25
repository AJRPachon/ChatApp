package com.ajrpachon.chatapp.utils

object UploadLimits {
    const val IMAGE_MAX_BYTES = 10 * 1024 * 1024L   // 10 MB
    const val AUDIO_MAX_BYTES = 25 * 1024 * 1024L   // 25 MB
    const val AVATAR_MAX_BYTES = 5 * 1024 * 1024L   //  5 MB

    fun ByteArray.checkImageSize() = check(size <= IMAGE_MAX_BYTES) {
        "La imagen supera el tamaño máximo permitido (10 MB)"
    }

    fun ByteArray.checkAudioSize() = check(size <= AUDIO_MAX_BYTES) {
        "El audio supera el tamaño máximo permitido (25 MB)"
    }

    fun ByteArray.checkAvatarSize() = check(size <= AVATAR_MAX_BYTES) {
        "La foto supera el tamaño máximo permitido (5 MB)"
    }
}
