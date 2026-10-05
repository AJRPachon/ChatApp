package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.AiRequestDTO
import com.ajrpachon.chatapp.data.remote.dto.AiResponseDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

// The Functions plugin's builder-lambda invoke() overload calls Ktor's raw setBody(), which has
// no content converter registered for arbitrary types in this client and fails at runtime with
// "Fail to prepare request body for sending". The invoke(function, body, headers) overload uses
// Supabase's own serializer.encode() instead, which works reliably -- but per its own docs it
// requires the JSON content-type header to be set explicitly.
private val jsonHeaders = Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }

/** Calls the `ai-assistant` Edge Function. */
class AiAssistantRemoteSource(private val supabase: SupabaseClient) {

    /** Sends [request] and returns the function's `result` text. Throws on any HTTP or decoding failure. */
    suspend fun ask(request: AiRequestDTO): String =
        supabase.functions.invoke("ai-assistant", request, headers = jsonHeaders)
            .body<AiResponseDTO>()
            .result
}
