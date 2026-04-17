package com.ajrpachon.chatapp.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ──────────────────────────────────────────────────────────────────

data class AuthState(
    val isLoading: Boolean = false,
    val currentUser: UserBO? = null,
    val needsUsername: Boolean = false,
    val usernameInput: String = "",
    val usernameError: String? = null,
    val error: String? = null,
)

// ── Intents ────────────────────────────────────────────────────────────────

sealed interface AuthIntent {
    data class SignInWithGoogle(val context: Context) : AuthIntent
    data class UsernameChanged(val value: String) : AuthIntent
    data object ConfirmUsername : AuthIntent
    data object SignOut : AuthIntent
    data object DismissError : AuthIntent
}

// ── Effects ────────────────────────────────────────────────────────────────

sealed interface AuthEffect {
    data object NavigateToHome : AuthEffect
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class AuthViewModel(
    private val supabase: SupabaseClient,
    private val setUsernameUseCase: SetUsernameUseCase,
    private val googleWebClientId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SignInWithGoogle -> signInWithGoogle(intent.context)
            is AuthIntent.UsernameChanged -> _state.update { it.copy(usernameInput = intent.value, usernameError = null) }
            is AuthIntent.ConfirmUsername -> confirmUsername()
            is AuthIntent.SignOut -> signOut()
            is AuthIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val credentialManager = CredentialManager.create(context)
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(googleWebClientId)
                            .build()
                    )
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleCredential.idToken

                supabase.auth.signInWith(IDToken) {
                    provider = Google
                    this.idToken = idToken
                }

                val session = supabase.auth.currentSessionOrNull()
                val userId = session?.user?.id ?: error("No user after sign-in")
                val email = session.user?.email ?: ""

                // Check if profile has username
                val profile = supabase.auth.currentUserOrNull()
                val hasUsername = profile?.userMetadata?.get("username") != null

                if (hasUsername) {
                    _effect.send(AuthEffect.NavigateToHome)
                } else {
                    _state.update { it.copy(needsUsername = true) }
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: "Sign-in failed") }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun confirmUsername() {
        val username = _state.value.usernameInput.trim()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                _state.update { it.copy(error = "Not authenticated", isLoading = false) }
                return@launch
            }
            setUsernameUseCase(userId, username)
                .onSuccess {
                    _state.update { it.copy(needsUsername = false) }
                    _effect.send(AuthEffect.NavigateToHome)
                }
                .onFailure { e ->
                    _state.update { it.copy(usernameError = e.message, isLoading = false) }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            supabase.auth.signOut()
            _state.update { AuthState() }
        }
    }
}
