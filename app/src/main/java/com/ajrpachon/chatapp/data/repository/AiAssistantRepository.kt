package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
// This class shares its simple name with the domain interface it implements — aliased to avoid
// a same-name clash (there is no "...Impl" suffix on this one, unlike its sibling repositories).
import com.ajrpachon.chatapp.domain.repository.AiAssistantRepository as AiAssistantRepositoryContract
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
private data class AiRequest(
    val action: String,
    val messages: List<String>? = null,
    val prompt: String? = null,
)

@Serializable
private data class AiResponse(val result: String)

// The Functions plugin's builder-lambda invoke() overload calls Ktor's raw setBody(), which has
// no content converter registered for arbitrary types in this client and fails at runtime with
// "Fail to prepare request body for sending". The invoke(function, body, headers) overload uses
// Supabase's own serializer.encode() instead, which works reliably -- but per its own docs it
// requires the JSON content-type header to be set explicitly.
private val jsonHeaders = Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }

class AiAssistantRepository(
    private val supabaseClient: SupabaseClient,
    private val analyticsTracker: AnalyticsTracker,
) : AiAssistantRepositoryContract {

    override suspend fun summarize(messageSnippets: List<String>): Result<String> = runCatching {
        val response = supabaseClient.functions.invoke(
            "ai-assistant",
            AiRequest(action = "summarize", messages = messageSnippets),
            headers = jsonHeaders,
        )
        response.body<AiResponse>().result
    }.onSuccess { logUsage(AnalyticsEvents.ACTION_SUMMARIZE) }

    override suspend fun suggestReply(lastMessage: String): Result<String> = runCatching {
        val response = supabaseClient.functions.invoke(
            "ai-assistant",
            AiRequest(action = "suggest", messages = listOf(lastMessage)),
            headers = jsonHeaders,
        )
        response.body<AiResponse>().result
    }.onSuccess { logUsage(AnalyticsEvents.ACTION_SUGGEST_REPLY) }

    override suspend fun freeform(prompt: String): Result<String> = runCatching {
        val response = supabaseClient.functions.invoke(
            "ai-assistant",
            AiRequest(action = "freeform", prompt = prompt),
            headers = jsonHeaders,
        )
        response.body<AiResponse>().result
    }.onSuccess { logUsage(AnalyticsEvents.ACTION_FREEFORM) }

    private fun logUsage(action: String) {
        analyticsTracker.logEvent(AnalyticsEvents.AI_ASSISTANT_USED, mapOf(AnalyticsEvents.PARAM_ACTION to action))
    }
}
