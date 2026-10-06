package com.ajrpachon.chatapp.data.repository

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ConversationFileExporterImplTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val authority = "${context.packageName}.fileprovider"
    private val exporter = ConversationFileExporterImpl(context, authority)

    // FileProvider resolves paths against the real file system, which behaves differently on
    // Windows and Linux under Robolectric; what this class owns is the write and the arguments.
    @Before
    fun setUp() {
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } answers {
            Uri.parse("content://${secondArg<String>()}/cache/${thirdArg<File>().name}")
        }
    }

    @After
    fun tearDown() = unmockkStatic(FileProvider::class)

    @Test
    fun `the text is written to the cache directory under the given name`() = runTest {
        exporter.writeAndShare("chat_c1.txt", "hola\nadios\n")

        assertEquals("hola\nadios\n", File(context.cacheDir, "chat_c1.txt").readText())
    }

    @Test
    fun `it returns the uri the file provider gives for that file`() = runTest {
        val file = slot<File>()
        every { FileProvider.getUriForFile(eq(context), eq(authority), capture(file)) } returns Uri.parse("content://x/y")

        val uri = exporter.writeAndShare("chat_c1.txt", "hola")

        assertEquals("content://x/y", uri)
        assertEquals(File(context.cacheDir, "chat_c1.txt"), file.captured)
    }

    @Test
    fun `exporting again replaces the previous file`() = runTest {
        exporter.writeAndShare("chat_c1.txt", "primero")
        exporter.writeAndShare("chat_c1.txt", "segundo")

        assertEquals("segundo", File(context.cacheDir, "chat_c1.txt").readText())
    }
}
