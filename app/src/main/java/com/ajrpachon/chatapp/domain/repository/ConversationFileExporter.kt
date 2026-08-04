package com.ajrpachon.chatapp.domain.repository

interface ConversationFileExporter {
    suspend fun writeAndShare(fileName: String, content: String): String
}
