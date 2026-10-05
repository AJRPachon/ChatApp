package com.ajrpachon.chatapp.data.remote.source

import android.content.Context
import android.content.Intent
import com.ajrpachon.chatapp.data.remote.dto.AuthSessionDTO
import com.ajrpachon.chatapp.data.remote.dto.MfaAssuranceDTO
import com.ajrpachon.chatapp.data.remote.dto.TotpEnrollmentDTO
import com.ajrpachon.chatapp.domain.model.IntegrityResultBO
import com.ajrpachon.chatapp.utils.IntegrityChecker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.functions.functions

/**
 * Every call to Supabase Auth (sessions, sign-in/up/out, MFA) and to the `delete-account` Edge
 * Function. [AuthRepositoryImpl][com.ajrpachon.chatapp.data.repository.AuthRepositoryImpl] adds the
 * orchestration around them (analytics, crash-reporter user id, wiping local data).
 */
class AuthRemoteSource(private val supabase: SupabaseClient) {

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    fun getCurrentSession(): AuthSessionDTO? {
        val session = supabase.auth.currentSessionOrNull() ?: return null
        val userId = session.user?.id ?: return null
        return AuthSessionDTO(userId = userId, email = session.user?.email)
    }

    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Returns true when the sign-up already produced a session (no e-mail confirmation pending). */
    suspend fun signUpWithEmail(email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        supabase.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
            nonce = rawNonce
        }
    }

    /** [global] signs the user out of every device, not just this one. */
    suspend fun signOut(global: Boolean = false) {
        if (global) supabase.auth.signOut(SignOutScope.GLOBAL) else supabase.auth.signOut()
    }

    /**
     * POST with no body: the Edge Function resolves the user from the Authorization JWT. Throws a
     * RestException (401/429/500) on failure.
     */
    suspend fun deleteAccount() {
        supabase.functions.invoke("delete-account")
    }

    suspend fun clearLocalSession() {
        supabase.auth.clearSession()
    }

    suspend fun checkIntegrity(context: Context): IntegrityResultBO =
        IntegrityChecker.check(context, supabase)

    /** Completes a sign-in that arrived through the `auth-callback` deep link (OAuth, e-mail confirmation). */
    fun handleAuthDeepLink(intent: Intent) {
        supabase.handleDeeplinks(intent)
    }

    // ── MFA ──────────────────────────────────────────────────────────────────

    suspend fun getMfaAssuranceLevel(): MfaAssuranceDTO {
        val aal = supabase.auth.mfa.getAuthenticatorAssuranceLevel()
        return MfaAssuranceDTO(current = aal.current.name.lowercase(), next = aal.next.name.lowercase())
    }

    suspend fun getVerifiedTotpFactorId(): String? {
        val factors = supabase.auth.mfa.retrieveFactorsForCurrentUser()
        return factors.firstOrNull { it.factorType == "totp" && it.isVerified }?.id
    }

    suspend fun createMfaChallenge(factorId: String): String =
        supabase.auth.mfa.createChallenge(factorId).id

    suspend fun verifyMfaChallenge(factorId: String, challengeId: String, code: String) {
        supabase.auth.mfa.verifyChallenge(factorId = factorId, challengeId = challengeId, code = code)
    }

    suspend fun enrollTotp(): TotpEnrollmentDTO {
        val response = supabase.auth.mfa.enroll(FactorType.TOTP)
        return TotpEnrollmentDTO(
            factorId = response.id,
            qrCodeSvg = response.data.qrCode,
            secret = response.data.secret,
        )
    }

    suspend fun unenrollFactor(factorId: String) {
        supabase.auth.mfa.unenroll(factorId)
    }
}
