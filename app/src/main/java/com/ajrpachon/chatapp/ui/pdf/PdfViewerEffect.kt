package com.ajrpachon.chatapp.ui.pdf

sealed interface PdfViewerEffect {
    data class SharePdf(val url: String) : PdfViewerEffect
}
