package com.ajrpachon.chatapp.ui.auth

import android.content.Context
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
)

sealed interface AuthIntent {
    data class SignInWithGoogle(val context: Context) : AuthIntent
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
}

sealed interface AuthEffect {
    data object NavigateToHome : AuthEffect
    data object OpenAddGoogleAccount : AuthEffect
}
