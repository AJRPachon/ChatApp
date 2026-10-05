package com.ajrpachon.chatapp.ui.auth

import com.ajrpachon.chatapp.domain.model.UserBO

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthState(
    val isLoading: Boolean = true,
    val currentUser: UserBO? = null,
    val needsUsername: Boolean = false,
    val usernameInput: String = "",
    val usernameError: String? = null,
    val error: String? = null,
    val authMode: AuthMode = AuthMode.SIGN_IN,
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val showEmailVerification: Boolean = false,
    val showRegisterSuggestion: Boolean = false,
    val needsMfaChallenge: Boolean = false,
    val mfaFactorId: String? = null,
    val mfaCodeInput: String = "",
    val mfaError: String? = null,
    val mfaIsLoading: Boolean = false,
)

sealed interface AuthIntent {
    data object SignInWithGoogle : AuthIntent

    /** The screen obtained a Google ID token for the nonce the ViewModel asked about. */
    data class GoogleTokenReceived(val idToken: String) : AuthIntent

    /** The screen could not get a credential; [noCredential] means the device has no Google account to offer. */
    data class GoogleSignInFailed(val message: String?, val noCredential: Boolean) : AuthIntent

    /** The credential request was cancelled before it finished (the screen went away). */
    data object GoogleSignInCancelled : AuthIntent
    data object SignInWithEmail : AuthIntent
    data object SignUpWithEmail : AuthIntent
    data class ToggleMode(val mode: AuthMode) : AuthIntent
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class ConfirmPasswordChanged(val value: String) : AuthIntent
    data class UsernameChanged(val value: String) : AuthIntent
    data object ConfirmUsername : AuthIntent
    data object SignOut : AuthIntent
    data object DismissError : AuthIntent
    data object DismissEmailVerification : AuthIntent
    data object SwitchToRegister : AuthIntent
    data class MfaCodeChanged(val value: String) : AuthIntent
    data object VerifyMfaCode : AuthIntent
}

sealed interface AuthEffect {
    data object NavigateToHome : AuthEffect
    data object OpenAddGoogleAccount : AuthEffect

    /** Ask Credential Manager (which needs the Activity) for a Google credential bound to [hashedNonce]. */
    data class RequestGoogleCredential(val hashedNonce: String) : AuthEffect
    data class IntegrityFailed(val reason: String) : AuthEffect
}
