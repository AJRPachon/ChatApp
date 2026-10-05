package com.ajrpachon.chatapp.utils

enum class UploadKind { IMAGE, AUDIO, AVATAR, FILE, VIDEO }

/**
 * Thrown when a file is over the limit for its [kind]. A subclass of [IllegalStateException] so the
 * `catchResult` blocks that already handle a failed `check` keep working; the UI turns it into a
 * translated message (see `Throwable.toUiText`).
 */
class UploadTooLargeException(val kind: UploadKind, val limitMb: Int) :
    IllegalStateException("${kind.name.lowercase()} exceeds the maximum upload size of $limitMb MB")

object UploadLimits {
    const val IMAGE_MAX_BYTES = 10 * 1024 * 1024L   // 10 MB
    const val AUDIO_MAX_BYTES = 25 * 1024 * 1024L   // 25 MB
    const val AVATAR_MAX_BYTES = 5 * 1024 * 1024L   //  5 MB
    const val FILE_MAX_BYTES = 50 * 1024 * 1024L    // 50 MB
    const val VIDEO_MAX_BYTES = 50 * 1024 * 1024L   // 50 MB

    private const val BYTES_PER_MB = 1024 * 1024L

    private fun maxBytes(kind: UploadKind) = when (kind) {
        UploadKind.IMAGE -> IMAGE_MAX_BYTES
        UploadKind.AUDIO -> AUDIO_MAX_BYTES
        UploadKind.AVATAR -> AVATAR_MAX_BYTES
        UploadKind.FILE -> FILE_MAX_BYTES
        UploadKind.VIDEO -> VIDEO_MAX_BYTES
    }

    /** Throws [UploadTooLargeException] when [sizeBytes] is over the limit for [kind]. */
    fun requireWithinLimit(kind: UploadKind, sizeBytes: Long) {
        val max = maxBytes(kind)
        if (sizeBytes > max) throw UploadTooLargeException(kind, (max / BYTES_PER_MB).toInt())
    }

    fun ByteArray.checkImageSize() = requireWithinLimit(UploadKind.IMAGE, size.toLong())

    fun ByteArray.checkAudioSize() = requireWithinLimit(UploadKind.AUDIO, size.toLong())

    fun ByteArray.checkAvatarSize() = requireWithinLimit(UploadKind.AVATAR, size.toLong())

    fun ByteArray.checkFileSize() = requireWithinLimit(UploadKind.FILE, size.toLong())

    fun ByteArray.checkVideoSize() = requireWithinLimit(UploadKind.VIDEO, size.toLong())
}
