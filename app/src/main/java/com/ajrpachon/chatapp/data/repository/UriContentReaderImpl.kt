package com.ajrpachon.chatapp.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.ajrpachon.chatapp.domain.model.UriMetadata
import com.ajrpachon.chatapp.domain.repository.UriContentReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UriContentReaderImpl(
    private val contentResolver: ContentResolver,
) : UriContentReader {

    override fun getMetadata(uri: String): UriMetadata {
        val parsedUri = Uri.parse(uri)
        val mimeType = contentResolver.getType(parsedUri)
        var displayName: String? = null
        var size: Long? = null
        contentResolver.query(parsedUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return UriMetadata(mimeType = mimeType, displayName = displayName, size = size)
    }

    override suspend fun readBytes(uri: String): ByteArray = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            ?: error("Cannot open URI: $uri")
    }
}
