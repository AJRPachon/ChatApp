package com.ajrpachon.chatapp.data.repository

import android.app.Application
import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UriContentReaderImplTest {

    private val resolver = mockk<ContentResolver>()
    private val reader = UriContentReaderImpl(resolver)
    private val uriString = "content://media/external/file/42"
    private val uri: Uri = Uri.parse(uriString)

    private fun givenRow(name: String?, size: Long?) {
        every { resolver.getType(uri) } returns "image/png"
        every { resolver.query(uri, null, null, null, null) } returns
            MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply { addRow(arrayOf<Any?>(name, size)) }
    }

    @Test
    fun `metadata combines the mime type, display name and size`() {
        givenRow("foto.png", 2048L)

        val meta = reader.getMetadata(uriString)

        assertEquals("image/png", meta.mimeType)
        assertEquals("foto.png", meta.displayName)
        assertEquals(2048L, meta.size)
    }

    @Test
    fun `metadata still carries the mime type when the provider returns no cursor`() {
        every { resolver.getType(uri) } returns "application/pdf"
        every { resolver.query(uri, null, null, null, null) } returns null

        val meta = reader.getMetadata(uriString)

        assertEquals("application/pdf", meta.mimeType)
        assertNull(meta.displayName)
        assertNull(meta.size)
    }

    @Test
    fun `metadata ignores columns the provider does not expose`() {
        every { resolver.getType(uri) } returns null
        every { resolver.query(uri, null, null, null, null) } returns
            MatrixCursor(arrayOf("other")).apply { addRow(arrayOf<Any?>("x")) }

        val meta = reader.getMetadata(uriString)

        assertNull(meta.mimeType)
        assertNull(meta.displayName)
        assertNull(meta.size)
    }

    @Test
    fun `readBytes returns the whole content`() = runTest {
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))

        assertEquals(listOf<Byte>(1, 2, 3), reader.readBytes(uriString).toList())
    }

    @Test
    fun `readBytes fails with the uri when it cannot be opened`() = runTest {
        every { resolver.openInputStream(uri) } returns null

        val failure = runCatching { reader.readBytes(uriString) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals("Cannot open URI: $uriString", failure?.message)
    }
}
