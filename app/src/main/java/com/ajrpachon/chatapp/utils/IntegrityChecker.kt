package com.ajrpachon.chatapp.utils

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.SecureRandom
import java.util.Base64

/**
 * Requests a Play Integrity token and verifies it server-side.
 *
 * Verification flow:
 * 1. Generate a random nonce (32 bytes, base64url-encoded)
 * 2. Request an integrity token from Google Play bound to that nonce
 * 3. Send the token to the `verify-integrity` Edge Function
 * 4. Edge Function decodes it via Google Play Integrity API and returns the verdict
 *
 * Returns true if the device/app passes all three integrity checks:
 * - appRecognitionVerdict == PLAY_RECOGNIZED
 * - deviceIntegrity contains MEETS_DEVICE_INTEGRITY
 * - accountDetails.appLicensingVerdict == LICENSED
 *
 * On emulators or sideloaded APKs the verdict will be different — callers
 * should decide the enforcement policy (block vs. warn vs. log).
 */
object IntegrityChecker {

    suspend fun check(context: Context, supabase: SupabaseClient): IntegrityResult {
        return runCatching {
            val nonce = generateNonce()
            val manager = IntegrityManagerFactory.create(context)
            val tokenResponse = manager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                    .setNonce(nonce)
                    .build()
            ).await()

            val responseText = supabase.functions.invoke(
                function = "verify-integrity",
                body = buildJsonObject {
                    put("token", tokenResponse.token())
                    put("nonce", nonce)
                },
            ).bodyAsText()

            val json = Json.parseToJsonElement(responseText).jsonObject
            val passed = json["passed"]?.jsonPrimitive?.content?.toBoolean() ?: false
            val reason = json["reason"]?.jsonPrimitive?.content

            if (passed) IntegrityResult.Passed else IntegrityResult.Failed(reason ?: "unknown")
        }.getOrElse { e ->
            IntegrityResult.Error(e.message ?: "integrity check failed")
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

sealed interface IntegrityResult {
    data object Passed : IntegrityResult
    data class Failed(val reason: String) : IntegrityResult
    data class Error(val message: String) : IntegrityResult
}
