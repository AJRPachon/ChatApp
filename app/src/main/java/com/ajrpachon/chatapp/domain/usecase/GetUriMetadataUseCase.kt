package com.ajrpachon.chatapp.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

data class UriMetadata(
    val mimeType: String?,
    val displayName: String?,
    val size: Long?,
)

class GetUriMetadataUseCase(private val contentResolver: ContentResolver) {
    operator fun invoke(uri: Uri): UriMetadata {
        val mimeType = contentResolver.getType(uri)
        var displayName: String? = null
        var size: Long? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return UriMetadata(mimeType = mimeType, displayName = displayName, size = size)
    }
}
