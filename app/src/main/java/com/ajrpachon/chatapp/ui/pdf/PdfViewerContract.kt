package com.ajrpachon.chatapp.ui.pdf

import androidx.compose.ui.graphics.ImageBitmap
import com.ajrpachon.chatapp.ui.common.UiText

data class PdfViewerState(
    val pages: List<ImageBitmap> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface PdfViewerEffect {
    data class SharePdf(val url: String) : PdfViewerEffect
}

sealed interface PdfViewerIntent {
    data class LoadPdf(val url: String) : PdfViewerIntent
    data class SharePdf(val url: String) : PdfViewerIntent
}
