package com.ajrpachon.chatapp.ui.pdf

import com.ajrpachon.chatapp.domain.usecase.GetCacheFilePathUseCase
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val okHttpClient = mockk<OkHttpClient>()

    private fun buildViewModel() = PdfViewerViewModel(
        getCacheFilePath = GetCacheFilePathUseCase(tempFolder.root.absolutePath),
        okHttpClient = okHttpClient,
    )

    private fun respondWith(code: Int) {
        val call = mockk<Call>()
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } answers {
            Response.Builder()
                .request(Request.Builder().url("https://example.com/a.pdf").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .build()
        }
    }

    /** The download runs on Dispatchers.IO, so let the test scheduler run while real time passes. */
    private fun TestScope.awaitState(vm: PdfViewerViewModel, predicate: (PdfViewerState) -> Boolean) {
        repeat(AWAIT_ATTEMPTS) {
            advanceUntilIdle()
            if (predicate(vm.state.value)) return
            Thread.sleep(AWAIT_STEP_MS)
        }
        error("state never matched: ${vm.state.value}")
    }

    @Test
    fun `starts empty and not loading`() {
        val state = buildViewModel().state.value

        assertTrue(state.pages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `an unsuccessful download ends loading with the http status as the error`() =
        runTest(mainDispatcherRule.scheduler) {
            respondWith(404)
            val vm = buildViewModel()

            vm.onIntent(PdfViewerIntent.LoadPdf("https://example.com/a.pdf"))
            awaitState(vm) { !it.isLoading && it.error != null }

            assertEquals(UiText.Dynamic("HTTP 404"), vm.state.value.error)
            assertTrue(vm.state.value.pages.isEmpty())
        }

    @Test
    fun `a network failure ends loading with an error`() = runTest(mainDispatcherRule.scheduler) {
        val call = mockk<Call>()
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } throws java.io.IOException("no route to host")
        val vm = buildViewModel()

        vm.loadPdf("https://example.com/a.pdf")
        awaitState(vm) { it.error != null }

        assertEquals(UiText.Dynamic("no route to host"), vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `loading again after a failure retries the download`() = runTest(mainDispatcherRule.scheduler) {
        respondWith(500)
        val vm = buildViewModel()
        vm.loadPdf("https://example.com/a.pdf")
        awaitState(vm) { it.error != null }

        respondWith(404)
        vm.loadPdf("https://example.com/a.pdf")
        awaitState(vm) { it.error == UiText.Dynamic("HTTP 404") }

        assertNotNull(vm.state.value.error)
        verify(atLeast = 2) { okHttpClient.newCall(any()) }
    }

    @Test
    fun `sharing emits the url as an effect`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()

        vm.onIntent(PdfViewerIntent.SharePdf("https://example.com/a.pdf"))
        advanceUntilIdle()

        assertEquals(PdfViewerEffect.SharePdf("https://example.com/a.pdf"), vm.effect.first())
    }

    private companion object {
        const val AWAIT_ATTEMPTS = 500
        const val AWAIT_STEP_MS = 10L
    }
}
