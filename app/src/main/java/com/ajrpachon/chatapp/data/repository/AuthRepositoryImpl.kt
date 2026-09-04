package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import com.ajrpachon.chatapp.utils.IntegrityChecker
import com.ajrpachon.chatapp.utils.IntegrityResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.SignOutScope

class AuthRepositoryImpl(
    private val context: android.content.Context,
    private val supabase: SupabaseClient,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : AuthRepository {

    override fun getCurrentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    override suspend fun getCurrentSessionInfo(): SessionInfo? {
        val session = supabase.auth.currentSessionOrNull() ?: return null
        val userId = session.user?.id ?: return null
        return SessionInfo(userId = userId, email = session.user?.email)
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        crashReporter.setUserId(getCurrentUserId())
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN,
            mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_EMAIL),
        )
    }

    override suspend fun signUpWithEmail(email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.SIGN_UP,
            mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_EMAIL),
        )
        return supabase.auth.currentSessionOrNull() != null
    }

    // Supabase auto-creates the account on first Google sign-in — there's no separate
    // "new account" signal from this call, so first-time Google users are logged as `login`
    // too (unlike email, where sign-up is a distinct explicit call).
    override suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        supabase.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
            nonce = rawNonce
        }
        crashReporter.setUserId(getCurrentUserId())
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN,
            mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_GOOGLE),
        )
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
        crashReporter.setUserId(null)
        analyticsTracker.logEvent(AnalyticsEvents.LOGOUT)
    }

    override suspend fun signOutAll() {
        supabase.auth.signOut(SignOutScope.GLOBAL)
        crashReporter.setUserId(null)
        analyticsTracker.logEvent(AnalyticsEvents.LOGOUT)
    }

    override suspend fun checkIntegrity(): IntegrityResult =
        IntegrityChecker.check(context, supabase)

    override suspend fun getMfaAssuranceLevel(): MfaAssuranceLevel? {
        val aal = supabase.auth.mfa.getAuthenticatorAssuranceLevel()
        return MfaAssuranceLevel(current = aal.current.name.lowercase(), next = aal.next.name.lowercase())
    }

    override suspend fun getVerifiedTotpFactorId(): String? {
        val factors = supabase.auth.mfa.retrieveFactorsForCurrentUser()
        return factors.firstOrNull { it.factorType == "totp" && it.isVerified }?.id
    }

    override suspend fun createMfaChallenge(factorId: String): String {
        val challenge = supabase.auth.mfa.createChallenge(factorId)
        return challenge.id
    }

    override suspend fun verifyMfaChallenge(factorId: String, challengeId: String, code: String) {
        supabase.auth.mfa.verifyChallenge(factorId = factorId, challengeId = challengeId, code = code)
    }

    override suspend fun enrollTotp(): TotpEnrollment {
        val response = supabase.auth.mfa.enroll(FactorType.TOTP)
        return TotpEnrollment(
            factorId = response.id,
            qrCodeSvg = response.data.qrCode,
            secret = response.data.secret,
        )
    }

    override suspend fun unenrollFactor(factorId: String) {
        supabase.auth.mfa.unenroll(factorId)
    }
}
