package com.ajrpachon.chatapp.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class PdfViewerState(
    val pages: List<ImageBitmap> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PdfViewerViewModel(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    fun loadPdf(url: String) {
        if (_state.value.isLoading || _state.value.pages.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = PdfViewerState(isLoading = true)
            runCatching { downloadAndRender(url) }
                .onSuccess { pages -> _state.value = PdfViewerState(pages = pages) }
                .onFailure { e ->
                    AppLogger.e("PdfViewerViewModel", "Failed to load PDF: ${e.message}")
                    _state.value = PdfViewerState(error = e.message ?: "Error loading PDF")
                }
        }
    }

    private suspend fun downloadAndRender(url: String): List<ImageBitmap> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "pdf_${url.hashCode()}.pdf")

        if (!cacheFile.exists()) {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.byteStream()?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Empty response body")
            }
        }

        val pages = mutableListOf<ImageBitmap>()
        val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd.use {
            val renderer = PdfRenderer(it)
            renderer.use {
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = 2f
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(bitmap.asImageBitmap())
                    }
                }
            }
        }
        pages
    }
}
