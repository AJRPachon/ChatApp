package com.ajrpachon.chatapp.domain.repository

interface AiAssistantRepository {
    suspend fun summarize(messageSnippets: List<String>): Result<String>
    suspend fun suggestReply(lastMessage: String): Result<String>
    suspend fun freeform(prompt: String): Result<String>
}
