package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMapperTest {

    private val message = MessageDBO(
        id = "m1",
        conversationId = "conv1",
        senderId = "u1",
        content = "hola",
        isRead = true,
        createdAt = 1_700_000_000_000L,
        imageUrl = "https://cdn/i.png",
        replyToId = "m0",
        replyToContent = "quoted",
        replyToSenderName = "Ana",
        isEncrypted = true,
        isEdited = true,
        editedAt = 1_700_000_100_000L,
        expiresAt = 1_700_999_000_000L,
        fileUrl = "https://cdn/f.pdf",
        fileName = "f.pdf",
        fileSize = 10L,
        fileMimeType = "application/pdf",
        isPinned = true,
        isSaved = true,
    )

    @Test
    fun `a message survives the trip to the backup format and back`() {
        assertEquals(message, message.toBackup().toDBO())
    }

    @Test
    fun `toBackup carries the identity and text of the message`() {
        val backup = message.toBackup()

        assertEquals("m1", backup.id)
        assertEquals("conv1", backup.conversationId)
        assertEquals("u1", backup.senderId)
        assertEquals("hola", backup.content)
        assertEquals(1_700_000_000_000L, backup.createdAt)
    }
}
