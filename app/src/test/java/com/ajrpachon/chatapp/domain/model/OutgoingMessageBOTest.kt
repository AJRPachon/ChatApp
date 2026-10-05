package com.ajrpachon.chatapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutgoingMessageTest {

    private fun text() = OutgoingMessageBO(conversationId = "c", senderId = "u", content = "hi")

    @Test
    fun `plain text and reply context carry no non-text payload`() {
        assertFalse(text().hasNonTextPayload)
        assertFalse(text().copy(replyToId = "m0", replyToContent = "q", replyToSenderName = "Ana").hasNonTextPayload)
    }

    @Test
    fun `every media, sticker, file and call field counts as non-text payload`() {
        val withPayload = listOf(
            text().copy(imageUrl = "i"),
            text().copy(audioUrl = "a"),
            text().copy(callType = "voice"),
            text().copy(gifUrl = "g"),
            text().copy(stickerUrl = "s"),
            text().copy(fileUrl = "f"),
            text().copy(videoUrl = "v"),
        )
        assertTrue(withPayload.all { it.hasNonTextPayload })
    }

    @Test
    fun `file metadata and E2EE target alone do not count as a payload`() {
        val message = text().copy(fileName = "doc.pdf", fileSize = 1L, fileMimeType = "application/pdf", otherUserId = "alice")
        assertFalse(message.hasNonTextPayload)
    }
}
