package com.ajrpachon.chatapp.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import com.ajrpachon.chatapp.domain.repository.ConversationFileExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ConversationFileExporterImpl(
    private val context: Context,
    private val fileProviderAuthority: String,
) : ConversationFileExporter {

    override suspend fun writeAndShare(fileName: String, content: String): String = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, fileName)
        FileOutputStream(outFile).use { it.write(content.toByteArray()) }
        FileProvider.getUriForFile(context, fileProviderAuthority, outFile).toString()
    }
}
