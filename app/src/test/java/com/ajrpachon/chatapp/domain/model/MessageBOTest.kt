package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBOTest {

    private fun message(
        content: String = "hello",
        expiresAt: Long? = null,
        callType: String? = null,
        stickerUrl: String? = null,
        gifUrl: String? = null,
        imageUrl: String? = null,
        audioUrl: String? = null,
        fileUrl: String? = null,
        fileName: String? = null,
        videoUrl: String? = null,
    ) = MessageBO(
        id = "msg-1",
        conversationId = "conv-1",
        senderId = "user-1",
        senderName = "Alice",
        content = content,
        isRead = true,
        isFromMe = false,
        createdAt = Instant.fromEpochMilliseconds(0L),
        expiresAt = expiresAt,
        callType = callType,
        stickerUrl = stickerUrl,
        gifUrl = gifUrl,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        fileUrl = fileUrl,
        fileName = fileName,
        videoUrl = videoUrl,
    )

    // ── isExpired ────────────────────────────────────────────────────────────

    @Test
    fun `isExpired is false when expiresAt is null`() {
        assertFalse(message(expiresAt = null).isExpired())
    }

    @Test
    fun `isExpired is true when expiresAt is in the past`() {
        assertTrue(message(expiresAt = System.currentTimeMillis() - 1000).isExpired())
    }

    @Test
    fun `isExpired is false when expiresAt is in the future`() {
        assertFalse(message(expiresAt = System.currentTimeMillis() + 60_000).isExpired())
    }

    // ── expiresInSeconds ─────────────────────────────────────────────────────

    @Test
    fun `expiresInSeconds is null when expiresAt is null`() {
        assertNull(message(expiresAt = null).expiresInSeconds())
    }

    @Test
    fun `expiresInSeconds is coerced to zero when already expired`() {
        val result = message(expiresAt = System.currentTimeMillis() - 60_000).expiresInSeconds()
        assertEquals(0L, result)
    }

    @Test
    fun `expiresInSeconds returns a positive remaining duration`() {
        val result = message(expiresAt = System.currentTimeMillis() + 10_000).expiresInSeconds()
        assertTrue(result != null && result in 0..10)
    }

    // ── isCallMessage ────────────────────────────────────────────────────────

    @Test
    fun `isCallMessage is true when callType is set`() {
        assertTrue(message(callType = "video").isCallMessage)
    }

    @Test
    fun `isCallMessage is false when callType is null`() {
        assertFalse(message(callType = null).isCallMessage)
    }

    // ── replySnippet ─────────────────────────────────────────────────────────

    @Test
    fun `replySnippet returns video call label`() {
        assertEquals("Videollamada", message(callType = "video").replySnippet())
    }

    @Test
    fun `replySnippet returns voice call label for non-video call types`() {
        assertEquals("Llamada de voz", message(callType = "audio").replySnippet())
    }

    @Test
    fun `replySnippet returns sticker label`() {
        assertEquals("Sticker", message(stickerUrl = "sticker.png").replySnippet())
    }

    @Test
    fun `replySnippet returns gif label`() {
        assertEquals("GIF", message(gifUrl = "gif.gif").replySnippet())
    }

    @Test
    fun `replySnippet returns image label`() {
        assertEquals("Imagen", message(imageUrl = "img.png").replySnippet())
    }

    @Test
    fun `replySnippet returns audio label`() {
        assertEquals("Audio", message(audioUrl = "audio.mp3").replySnippet())
    }

    @Test
    fun `replySnippet returns file label with name`() {
        assertEquals("📎 doc.pdf", message(fileUrl = "doc.pdf", fileName = "doc.pdf").replySnippet())
    }

    @Test
    fun `replySnippet returns generic file label when name is missing`() {
        assertEquals("📎 Archivo", message(fileUrl = "doc.pdf", fileName = null).replySnippet())
    }

    @Test
    fun `replySnippet returns video label`() {
        assertEquals("🎥 Video", message(videoUrl = "video.mp4").replySnippet())
    }

    @Test
    fun `replySnippet extracts contact name from contact content`() {
        val bo = message(content = "contact:{\"name\":\"Bob Smith\",\"phone\":\"123\"}")
        assertEquals("👤 Bob Smith", bo.replySnippet())
    }

    @Test
    fun `replySnippet falls back to generic contact label when name is missing`() {
        val bo = message(content = "contact:{\"phone\":\"123\"}")
        assertEquals("👤 Contacto", bo.replySnippet())
    }

    @Test
    fun `replySnippet returns poll label`() {
        assertEquals("📊 Encuesta", message(content = "poll:{\"question\":\"?\"}").replySnippet())
    }

    @Test
    fun `replySnippet returns plain content when nothing else matches`() {
        assertEquals("hello", message(content = "hello").replySnippet())
    }

    @Test
    fun `replySnippet truncates long plain content to 80 characters`() {
        val longContent = "a".repeat(120)
        val result = message(content = longContent).replySnippet()
        assertEquals(80, result.length)
        assertEquals(longContent.take(80), result)
    }
}
