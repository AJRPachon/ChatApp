package com.ajrpachon.chatapp.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class AiAssistantRepository(private val supabaseClient: SupabaseClient) {

    suspend fun summarize(messageSnippets: List<String>): Result<String> = runCatching {
        val body = buildJsonObject {
            put("action", "summarize")
            putJsonArray("messages") {
                messageSnippets.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            }
        }
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(body)
        }
        response.body<String>()
    }

    suspend fun suggestReply(lastMessage: String): Result<String> = runCatching {
        val body = buildJsonObject {
            put("action", "suggest_reply")
            put("lastMessage", lastMessage)
        }
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(body)
        }
        response.body<String>()
    }

    suspend fun freeform(prompt: String): Result<String> = runCatching {
        val body = buildJsonObject {
            put("action", "freeform")
            put("prompt", prompt)
        }
        val response = supabaseClient.functions.invoke("ai-assistant") {
            setBody(body)
        }
        response.body<String>()
    }
}
