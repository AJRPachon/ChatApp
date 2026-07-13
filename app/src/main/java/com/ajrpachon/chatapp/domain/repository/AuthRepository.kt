package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.utils.IntegrityResult

data class SessionInfo(val userId: String, val email: String?)
data class MfaAssuranceLevel(val current: String, val next: String)
data class TotpEnrollment(val factorId: String, val qrCodeSvg: String, val secret: String)

interface AuthRepository {
    fun getCurrentUserId(): String?
    suspend fun getCurrentSessionInfo(): SessionInfo?
    suspend fun signInWithEmail(email: String, password: String)
    /** Returns true if a session was immediately created (no e-mail verification needed). */
    suspend fun signUpWithEmail(email: String, password: String): Boolean
    suspend fun signInWithGoogle(idToken: String, rawNonce: String)
    suspend fun signOut()
    suspend fun signOutAll()
    suspend fun checkIntegrity(): IntegrityResult
    // MFA
    suspend fun getMfaAssuranceLevel(): MfaAssuranceLevel?
    suspend fun getVerifiedTotpFactorId(): String?
    suspend fun createMfaChallenge(factorId: String): String
    suspend fun verifyMfaChallenge(factorId: String, challengeId: String, code: String)
    suspend fun enrollTotp(): TotpEnrollment
    suspend fun unenrollFactor(factorId: String)
}
