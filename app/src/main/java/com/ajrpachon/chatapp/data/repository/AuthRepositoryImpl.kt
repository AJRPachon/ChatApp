package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.ChatDatabase
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.source.AuthRemoteSource
import com.ajrpachon.chatapp.domain.model.IntegrityResultBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import com.ajrpachon.chatapp.utils.E2EEKeyManager
import com.ajrpachon.chatapp.utils.SessionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val context: android.content.Context,
    private val remote: AuthRemoteSource,
    private val chatDatabase: ChatDatabase,
    private val sessionGuard: SessionGuard,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : AuthRepository {

    override fun getCurrentUserId(): String? = remote.getCurrentUserId()

    override suspend fun getCurrentSessionInfo(): SessionInfo? =
        remote.getCurrentSession()?.toBO()

    override suspend fun signInWithEmail(email: String, password: String) {
        remote.signInWithEmail(email, password)
        crashReporter.setUserId(getCurrentUserId())
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN,
            mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_EMAIL),
        )
    }

    override suspend fun signUpWithEmail(email: String, password: String): Boolean {
        val hasSession = remote.signUpWithEmail(email, password)
        analyticsTracker.logEvent(
            AnalyticsEvents.SIGN_UP,
            mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_EMAIL),
        )
        return hasSession
    }

    // Supabase auto-creates the account on first Google sign-in — there's no separate
    // "new account" signal from this call, so first-time Google users are logged as `login`
    // too (unlike email, where sign-up is a distinct explicit call).
    override suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        remote.signInWithGoogle(idToken, rawNonce)
        crashReporter.setUserId(getCurrentUserId())
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN,
            mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_GOOGLE),
        )
    }

    override suspend fun signOut() {
        remote.signOut()
        crashReporter.setUserId(null)
        analyticsTracker.logEvent(AnalyticsEvents.LOGOUT)
    }

    override suspend fun signOutAll() {
        remote.signOut(global = true)
        crashReporter.setUserId(null)
        analyticsTracker.logEvent(AnalyticsEvents.LOGOUT)
    }

    override suspend fun deleteAccount() {
        val userId = getCurrentUserId()

        // Throws on failure (401/429/500); nothing below runs then.
        remote.deleteAccount()

        // From here on the server-side account/session is gone. Wipe everything local so the
        // device looks freshly installed for this user: Room cache (SQLCipher), the "remember me"
        // inactivity timer, this user's E2EE keypair, and the local Supabase session.
        withContext(Dispatchers.IO) { chatDatabase.clearAllTables() }
        if (userId != null) {
            E2EEKeyManager.deleteKeyPair(userId)
        }
        sessionGuard.clearSession()
        remote.clearLocalSession()
    }

    override suspend fun checkIntegrity(): IntegrityResultBO = remote.checkIntegrity(context)

    override suspend fun getMfaAssuranceLevel(): MfaAssuranceLevel? = remote.getMfaAssuranceLevel().toBO()

    override suspend fun getVerifiedTotpFactorId(): String? = remote.getVerifiedTotpFactorId()

    override suspend fun createMfaChallenge(factorId: String): String = remote.createMfaChallenge(factorId)

    override suspend fun verifyMfaChallenge(factorId: String, challengeId: String, code: String) {
        remote.verifyMfaChallenge(factorId, challengeId, code)
    }

    override suspend fun enrollTotp(): TotpEnrollment = remote.enrollTotp().toBO()

    override suspend fun unenrollFactor(factorId: String) {
        remote.unenrollFactor(factorId)
    }
}
