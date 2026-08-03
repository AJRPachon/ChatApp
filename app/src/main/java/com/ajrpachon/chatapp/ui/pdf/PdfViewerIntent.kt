package com.ajrpachon.chatapp.ui.pdf

sealed interface PdfViewerIntent {
    data class LoadPdf(val url: String) : PdfViewerIntent
    data class SharePdf(val url: String) : PdfViewerIntent
}
