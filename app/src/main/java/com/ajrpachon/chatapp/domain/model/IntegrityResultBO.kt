package com.ajrpachon.chatapp.domain.model

/**
 * Outcome of the device-integrity check (Play Integrity, verified server-side by the
 * `verify-integrity` Edge Function). Produced by `utils/IntegrityChecker` and exposed through
 * `AuthRepository.checkIntegrity()`.
 */
sealed interface IntegrityResultBO {
    data object Passed : IntegrityResultBO
    data class Failed(val reason: String) : IntegrityResultBO
    data class Error(val message: String) : IntegrityResultBO
}
