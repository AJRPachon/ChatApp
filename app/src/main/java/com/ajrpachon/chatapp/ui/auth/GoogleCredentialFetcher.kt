package com.ajrpachon.chatapp.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.ajrpachon.chatapp.utils.catchResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * The OAuth web client ID Google Sign-In needs. A wrapper around the `BuildConfig` string so Koin can
 * inject it by type (a bare `String` would be ambiguous), which keeps `viewModelOf` usable.
 */
data class GoogleSignInConfig(val webClientId: String)

sealed interface GoogleCredentialResult {
    data class Token(val idToken: String) : GoogleCredentialResult

    /** The device has no Google account the credential sheet could offer. */
    data object NoCredential : GoogleCredentialResult

    data class Failed(val message: String?) : GoogleCredentialResult
}

/**
 * Asks Credential Manager for a Google ID token. Credential Manager shows its sheet over an
 * Activity, so the [Context] has to come from the screen on every call; that is why this is not
 * done inside [AuthViewModel], which must not hold a Context.
 */
class GoogleCredentialFetcher(
    private val credentialManager: CredentialManager,
    private val config: GoogleSignInConfig,
) {

    /**
     * Tries the one-tap sheet for already-authorised accounts first and, if the device has no
     * credential for it, the full "Sign in with Google" sheet. [hashedNonce] is the SHA-256 of the
     * raw nonce the caller keeps to verify the token with Supabase.
     */
    suspend fun fetch(context: Context, hashedNonce: String): GoogleCredentialResult {
        val oneTap = catchResult {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(config.webClientId)
                        .setNonce(hashedNonce)
                        .build()
                ).build()
            credentialManager.getCredential(context, request)
        }
        val credential = if (oneTap.exceptionOrNull() is NoCredentialException) {
            catchResult {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetSignInWithGoogleOption.Builder(config.webClientId)
                            .setNonce(hashedNonce)
                            .build()
                    ).build()
                credentialManager.getCredential(context, request)
            }
        } else {
            oneTap
        }
        return credential.fold(
            onSuccess = { result ->
                catchResult { GoogleIdTokenCredential.createFrom(result.credential.data).idToken }
                    .fold({ GoogleCredentialResult.Token(it) }, { GoogleCredentialResult.Failed(it.message) })
            },
            onFailure = { e ->
                if (e is NoCredentialException) GoogleCredentialResult.NoCredential else GoogleCredentialResult.Failed(e.message)
            },
        )
    }
}
