package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.ChatDatabase
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment
import com.ajrpachon.chatapp.utils.E2EEKeyManager
import com.ajrpachon.chatapp.utils.IntegrityChecker
import com.ajrpachon.chatapp.utils.IntegrityResult
import com.ajrpachon.chatapp.utils.SessionGuard
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val context: android.content.Context,
    private val supabase: SupabaseClient,
    private val chatDatabase: ChatDatabase,
    private val sessionGuard: SessionGuard,
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
    }

    override suspend fun signUpWithEmail(email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return supabase.auth.currentSessionOrNull() != null
    }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        supabase.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
            nonce = rawNonce
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override suspend fun signOutAll() {
        supabase.auth.signOut(SignOutScope.GLOBAL)
    }

    override suspend fun deleteAccount() {
        val userId = getCurrentUserId()

        // POST, no body — the Edge Function resolves the user from the Authorization JWT.
        // Throws a RestException (statusCode 401/429/500) on failure; nothing below runs then.
        supabase.functions.invoke("delete-account")

        // From here on the server-side account/session is gone. Wipe everything local so the
        // device looks freshly installed for this user: Room cache (SQLCipher), the "remember me"
        // inactivity timer, this user's E2EE keypair, and the local Supabase session.
        withContext(Dispatchers.IO) { chatDatabase.clearAllTables() }
        if (userId != null) {
            E2EEKeyManager.deleteKeyPair(userId)
        }
        sessionGuard.clearSession()
        supabase.auth.clearSession()
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
