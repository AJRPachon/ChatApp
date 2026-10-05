package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.domain.model.OutgoingMessageBO
import com.ajrpachon.chatapp.domain.model.StatusReplyContextBO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageMapperTest {

    // ── OutgoingMessageBO.toDTO ────────────────────────────────────

    @Test
    fun `OutgoingMessageBO toDTO carries identity, payload and reply fields`() {
        val outgoing = OutgoingMessageBO(
            conversationId = "conv1",
            senderId = "user1",
            content = "plain text",
            fileUrl = FILE_URL,
            fileName = "doc.pdf",
            fileSize = 10L,
            fileMimeType = "application/pdf",
            replyToId = "m0",
            replyToContent = "quoted",
            replyToSenderName = "Ana",
        )

        val dto = outgoing.toDTO(id = "id1", createdAt = "2026-10-05T10:00:00Z", content = "plain text", isEncrypted = false)

        assertEquals("id1", dto.id)
        assertEquals("conv1", dto.conversationId)
        assertEquals("user1", dto.senderId)
        assertEquals("2026-10-05T10:00:00Z", dto.createdAt)
        assertFalse(dto.isRead)
        assertEquals(FILE_URL, dto.fileUrl)
        assertEquals("doc.pdf", dto.fileName)
        assertEquals(10L, dto.fileSize)
        assertEquals("m0", dto.replyToId)
        assertEquals("quoted", dto.replyToContent)
        assertEquals("Ana", dto.replyToSenderName)
        assertNull(dto.imageUrl)
        assertNull(dto.replyToStatusId)
    }

    @Test
    fun `OutgoingMessageBO toDTO uses the content it is given, not the plaintext, and flags encryption`() {
        val outgoing = OutgoingMessageBO(conversationId = "c", senderId = "u", content = "secret")

        val dto = outgoing.toDTO(id = "id1", createdAt = "2026-10-05T10:00:00Z", content = "ciphertext", isEncrypted = true)

        assertEquals("ciphertext", dto.content)
        assertTrue(dto.isEncrypted)
    }

    @Test
    fun `OutgoingMessageBO toDTO snapshots the status being replied to`() {
        val outgoing = OutgoingMessageBO(
            conversationId = "c",
            senderId = "u",
            content = "nice!",
            statusReply = StatusReplyContextBO(
                statusId = "s1",
                statusOwnerId = "alice",
                statusText = "hola",
                statusImageUrl = null,
                statusVideoUrl = null,
                statusBackgroundColor = 0xFF112233L,
                statusExpiresAt = 1_700_000_000_000L,
            ),
        )

        val dto = outgoing.toDTO(id = "id1", createdAt = "2026-10-05T10:00:00Z", content = "nice!", isEncrypted = false)

        assertEquals("s1", dto.replyToStatusId)
        assertEquals("alice", dto.replyToStatusOwnerId)
        assertEquals("hola", dto.replyToStatusText)
        assertEquals(0xFF112233L, dto.replyToStatusBackgroundColor)
        assertEquals("2023-11-14T22:13:20Z", dto.replyToStatusExpiresAt)
    }

    // ── toDBO — file fields ───────────────────────────────────────────────────

    @Test
    fun `toDBO maps file fields from DTO`() {
        val dto = fakeDto(
            fileUrl = FILE_URL,
            fileName = "doc.pdf",
            fileSize = 204800L,
            fileMimeType = "application/pdf",
        )

        val dbo = dto.toDBO()

        assertEquals(FILE_URL, dbo.fileUrl)
        assertEquals("doc.pdf", dbo.fileName)
        assertEquals(204800L, dbo.fileSize)
        assertEquals("application/pdf", dbo.fileMimeType)
    }

    @Test
    fun `toDBO maps null file fields when absent`() {
        val dbo = fakeDto().toDBO()

        assertNull(dbo.fileUrl)
        assertNull(dbo.fileName)
        assertNull(dbo.fileSize)
        assertNull(dbo.fileMimeType)
    }

    @Test
    fun `toDBO sanitizes malicious fileUrl`() {
        val dto = fakeDto(fileUrl = BAD_URL_JS)
        val dbo = dto.toDBO()
        assertNull(dbo.fileUrl)
    }

    // ── toDBO — video fields ──────────────────────────────────────────────────

    @Test
    fun `toDBO maps videoUrl from DTO`() {
        val dto = fakeDto(videoUrl = VIDEO_URL)
        val dbo = dto.toDBO()
        assertEquals(VIDEO_URL, dbo.videoUrl)
    }

    @Test
    fun `toDBO maps null videoUrl when absent`() {
        val dbo = fakeDto().toDBO()
        assertNull(dbo.videoUrl)
    }

    @Test
    fun `toDBO sanitizes malicious videoUrl`() {
        val dto = fakeDto(videoUrl = BAD_URL_FTP)
        val dbo = dto.toDBO()
        assertNull(dbo.videoUrl)
    }

    // ── audio amplitudes (waveform, always visible on received voice messages) ─────

    @Test
    fun `toDBO maps audioAmplitudes from DTO`() {
        val dto = fakeDto().let { it.copy(audioAmplitudes = "0.100,0.900,0.400") }
        val dbo = dto.toDBO()
        assertEquals("0.100,0.900,0.400", dbo.audioAmplitudes)
    }

    @Test
    fun `toBO maps audioAmplitudes from DBO`() {
        val dbo = fakeDbo().let { it.copy(audioAmplitudes = "0.100,0.900,0.400") }
        val bo = dbo.toBO("user1", "Alice")
        assertEquals("0.100,0.900,0.400", bo.audioAmplitudes)
    }

    @Test
    fun `toBO maps null audioAmplitudes when absent`() {
        val bo = fakeDbo().toBO("user1", "Alice")
        assertNull(bo.audioAmplitudes)
    }

    // ── toBO — file fields ────────────────────────────────────────────────────

    @Test
    fun `toBO maps file fields from DBO`() {
        val dbo = fakeDbo(
            fileUrl = FILE_URL,
            fileName = "doc.pdf",
            fileSize = 204800L,
            fileMimeType = "application/pdf",
        )

        val bo = dbo.toBO("user1", "Alice")

        assertEquals(FILE_URL, bo.fileUrl)
        assertEquals("doc.pdf", bo.fileName)
        assertEquals(204800L, bo.fileSize)
        assertEquals("application/pdf", bo.fileMimeType)
    }

    @Test
    fun `toBO maps null file fields when absent`() {
        val bo = fakeDbo().toBO("user1", "Alice")

        assertNull(bo.fileUrl)
        assertNull(bo.fileName)
        assertNull(bo.fileSize)
        assertNull(bo.fileMimeType)
    }

    // ── toBO — video fields ───────────────────────────────────────────────────

    @Test
    fun `toBO maps videoUrl from DBO`() {
        val dbo = fakeDbo(videoUrl = VIDEO_URL)
        val bo = dbo.toBO("user1", "Alice")
        assertEquals(VIDEO_URL, bo.videoUrl)
    }

    @Test
    fun `toBO maps null videoUrl when absent`() {
        val bo = fakeDbo().toBO("user1", "Alice")
        assertNull(bo.videoUrl)
    }

    // ── replySnippet ──────────────────────────────────────────────────────────

    @Test
    fun `replySnippet returns file label for file messages`() {
        val bo = fakeDbo(fileUrl = FILE_URL, fileName = "report.pdf").toBO("user1", "Alice")
        assertEquals("📎 report.pdf", bo.replySnippet())
    }

    @Test
    fun `replySnippet returns generic file label when fileName is null`() {
        val bo = fakeDbo(fileUrl = FILE_URL, fileName = null).toBO("user1", "Alice")
        assertEquals("📎 Archivo", bo.replySnippet())
    }

    @Test
    fun `replySnippet returns video label for video messages`() {
        val bo = fakeDbo(videoUrl = VIDEO_URL).toBO("user1", "Alice")
        assertEquals("🎥 Video", bo.replySnippet())
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    companion object {
        // Must use whitelisted Supabase host so MediaUrlValidator.sanitize() passes through
        private const val FILE_URL = "https://xyzproject.supabase.co/storage/v1/object/public/chat-files/doc.pdf"
        private const val VIDEO_URL = "https://xyzproject.supabase.co/storage/v1/object/public/chat-videos/clip.mp4"
        private const val BAD_URL_JS = "javascript:alert(1)"
        private const val BAD_URL_FTP = "ftp://malicious.com/file"
    }

    private fun fakeDto(
        fileUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        fileMimeType: String? = null,
        videoUrl: String? = null,
    ) = MessageDTO(
        id = "msg1",
        conversationId = "conv1",
        senderId = "user1",
        content = "test",
        isRead = false,
        createdAt = "2024-01-01T00:00:00Z",
        fileUrl = fileUrl,
        fileName = fileName,
        fileSize = fileSize,
        fileMimeType = fileMimeType,
        videoUrl = videoUrl,
    )

    private fun fakeDbo(
        fileUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        fileMimeType: String? = null,
        videoUrl: String? = null,
    ) = MessageDBO(
        id = "msg1",
        conversationId = "conv1",
        senderId = "user1",
        content = "test",
        isRead = false,
        createdAt = System.currentTimeMillis(),
        fileUrl = fileUrl,
        fileName = fileName,
        fileSize = fileSize,
        fileMimeType = fileMimeType,
        videoUrl = videoUrl,
    )
}
