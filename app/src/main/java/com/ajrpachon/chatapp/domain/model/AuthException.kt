package com.ajrpachon.chatapp.domain.model

/** Why an authentication or account call failed, in terms the app understands rather than the backend's. */
enum class AuthErrorKind {
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    EMAIL_ALREADY_REGISTERED,

    /** The session is no longer valid (HTTP 401). */
    SESSION_EXPIRED,

    /** Too many attempts in a short time (HTTP 429). */
    TOO_MANY_REQUESTS,

    /** The server failed to process the request (HTTP 500). */
    SERVER_ERROR,
}

/**
 * What the auth repository throws for a failure it recognises. The message is the original one, so
 * a caller that does not care about the [kind] can still show it.
 */
class AuthException(val kind: AuthErrorKind, cause: Throwable? = null) : Exception(cause?.message, cause)
