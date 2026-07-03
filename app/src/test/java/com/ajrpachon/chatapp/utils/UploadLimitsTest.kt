package com.ajrpachon.chatapp.utils

import com.ajrpachon.chatapp.utils.UploadLimits.checkAudioSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkAvatarSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkFileSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkImageSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkVideoSize
import org.junit.Assert.assertThrows
import org.junit.Test

class UploadLimitsTest {

    // ── IMAGE ─────────────────────────────────────────────────────────────────

    @Test
    fun `image at exact limit passes`() {
        val data = ByteArray(UploadLimits.IMAGE_MAX_BYTES.toInt())
        data.checkImageSize() // should not throw
    }

    @Test
    fun `image one byte over limit throws`() {
        val data = ByteArray(UploadLimits.IMAGE_MAX_BYTES.toInt() + 1)
        assertThrows(IllegalStateException::class.java) { data.checkImageSize() }
    }

    @Test
    fun `image well under limit passes`() {
        val data = ByteArray(1024) // 1 KB
        data.checkImageSize()
    }

    // ── AUDIO ─────────────────────────────────────────────────────────────────

    @Test
    fun `audio at exact limit passes`() {
        val data = ByteArray(UploadLimits.AUDIO_MAX_BYTES.toInt())
        data.checkAudioSize()
    }

    @Test
    fun `audio one byte over limit throws`() {
        val data = ByteArray(UploadLimits.AUDIO_MAX_BYTES.toInt() + 1)
        assertThrows(IllegalStateException::class.java) { data.checkAudioSize() }
    }

    // ── AVATAR ────────────────────────────────────────────────────────────────

    @Test
    fun `avatar at exact limit passes`() {
        val data = ByteArray(UploadLimits.AVATAR_MAX_BYTES.toInt())
        data.checkAvatarSize()
    }

    @Test
    fun `avatar one byte over limit throws`() {
        val data = ByteArray(UploadLimits.AVATAR_MAX_BYTES.toInt() + 1)
        assertThrows(IllegalStateException::class.java) { data.checkAvatarSize() }
    }

    @Test
    fun `avatar well under limit passes`() {
        val data = ByteArray(512 * 1024) // 512 KB
        data.checkAvatarSize()
    }

    // ── FILE ──────────────────────────────────────────────────────────────────

    @Test
    fun `file at exact limit passes`() {
        val data = ByteArray(UploadLimits.FILE_MAX_BYTES.toInt())
        data.checkFileSize()
    }

    @Test
    fun `file one byte over limit throws`() {
        val data = ByteArray(UploadLimits.FILE_MAX_BYTES.toInt() + 1)
        assertThrows(IllegalStateException::class.java) { data.checkFileSize() }
    }

    // ── VIDEO ─────────────────────────────────────────────────────────────────

    @Test
    fun `video at exact limit passes`() {
        val data = ByteArray(UploadLimits.VIDEO_MAX_BYTES.toInt())
        data.checkVideoSize()
    }

    @Test
    fun `video one byte over limit throws`() {
        val data = ByteArray(UploadLimits.VIDEO_MAX_BYTES.toInt() + 1)
        assertThrows(IllegalStateException::class.java) { data.checkVideoSize() }
    }

    @Test
    fun `video well under limit passes`() {
        val data = ByteArray(1024 * 1024) // 1 MB
        data.checkVideoSize()
    }

    // ── limit constants sanity check ──────────────────────────────────────────

    @Test
    fun `image limit is 10 MB`() {
        assert(UploadLimits.IMAGE_MAX_BYTES == 10 * 1024 * 1024L)
    }

    @Test
    fun `audio limit is 25 MB`() {
        assert(UploadLimits.AUDIO_MAX_BYTES == 25 * 1024 * 1024L)
    }

    @Test
    fun `avatar limit is 5 MB`() {
        assert(UploadLimits.AVATAR_MAX_BYTES == 5 * 1024 * 1024L)
    }

    @Test
    fun `file limit is 50 MB`() {
        assert(UploadLimits.FILE_MAX_BYTES == 50 * 1024 * 1024L)
    }

    @Test
    fun `video limit is 50 MB`() {
        assert(UploadLimits.VIDEO_MAX_BYTES == 50 * 1024 * 1024L)
    }
}
