package com.ajrpachon.chatapp.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReadUriAsBytesUseCase(private val contentResolver: ContentResolver) {
    suspend operator fun invoke(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot open URI: $uri")
    }
}
