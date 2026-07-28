package com.ajrpachon.chatapp.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable
private data class AiRequest(
    val action: String,
    val messages: List<String>? = null,
    val prompt: String? = null,
)

@Serializable
private data class AiResponse(val result: String)

class AiAssistantRepository(private val supabaseClient: SupabaseClient) :
    com.ajrpachon.chatapp.domain.repository.AiAssistantRepository {

    override suspend fun summarize(messageSnippets: List<String>): Result<String> = runCatching {
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(AiRequest(action = "summarize", messages = messageSnippets))
        }
        response.body<AiResponse>().result
    }

    override suspend fun suggestReply(lastMessage: String): Result<String> = runCatching {
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(AiRequest(action = "suggest", messages = listOf(lastMessage)))
        }
        response.body<AiResponse>().result
    }

    override suspend fun freeform(prompt: String): Result<String> = runCatching {
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(AiRequest(action = "freeform", prompt = prompt))
        }
        response.body<AiResponse>().result
    }
}
