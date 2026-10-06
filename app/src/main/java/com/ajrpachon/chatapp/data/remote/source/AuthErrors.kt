package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.domain.model.AuthErrorKind
import com.ajrpachon.chatapp.domain.model.AuthException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500

/**
 * Turns a Supabase failure into an [AuthException] when it is one the app reacts to; anything else
 * is returned unchanged. This is the one place the UI's view of "wrong password" or "too many
 * attempts" meets Supabase's error codes, so ViewModels never have to know about `RestException`.
 */
internal fun Throwable.toAuthException(): Throwable {
    if (this is AuthException || this is CancellationException) return this
    val code = (this as? RestException)?.error
    val kind = when {
        code.equals("invalid_credentials", ignoreCase = true) || mentions("Invalid login") ->
            AuthErrorKind.INVALID_CREDENTIALS
        code.equals("email_not_confirmed", ignoreCase = true) || mentions("Email not confirmed") ->
            AuthErrorKind.EMAIL_NOT_CONFIRMED
        code.equals("user_already_exists", ignoreCase = true) ||
            mentions("already registered") || mentions("already been registered") ->
            AuthErrorKind.EMAIL_ALREADY_REGISTERED
        else -> when ((this as? RestException)?.statusCode) {
            HTTP_UNAUTHORIZED -> AuthErrorKind.SESSION_EXPIRED
            HTTP_TOO_MANY_REQUESTS -> AuthErrorKind.TOO_MANY_REQUESTS
            HTTP_SERVER_ERROR -> AuthErrorKind.SERVER_ERROR
            else -> null
        }
    }
    return if (kind != null) AuthException(kind, this) else this
}

private fun Throwable.mentions(text: String) = message?.contains(text, ignoreCase = true) == true

/** Runs [block] and rethrows any recognised failure as an [AuthException]. */
internal inline fun <T> mappingAuthErrors(block: () -> T): T =
    try {
        block()
    } catch (e: Throwable) {
        throw e.toAuthException()
    }
