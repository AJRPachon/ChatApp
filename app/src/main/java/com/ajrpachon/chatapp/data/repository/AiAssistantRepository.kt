package com.ajrpachon.chatapp.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Serializable
private data class AiResponse(val result: String)

class AiAssistantRepository(private val supabaseClient: SupabaseClient) :
    com.ajrpachon.chatapp.domain.repository.AiAssistantRepository {

    override suspend fun summarize(messageSnippets: List<String>): Result<String> = runCatching {
        val body = buildJsonObject {
            put("action", "summarize")
            putJsonArray("messages") {
                messageSnippets.forEach { add(JsonPrimitive(it)) }
            }
        }
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(body)
        }
        response.body<AiResponse>().result
    }

    override suspend fun suggestReply(lastMessage: String): Result<String> = runCatching {
        val body = buildJsonObject {
            put("action", "suggest")
            putJsonArray("messages") {
                add(JsonPrimitive(lastMessage))
            }
        }
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(body)
        }
        response.body<AiResponse>().result
    }

    override suspend fun freeform(prompt: String): Result<String> = runCatching {
        val body = buildJsonObject {
            put("action", "freeform")
            put("prompt", prompt)
        }
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(body)
        }
        response.body<AiResponse>().result
    }
}
