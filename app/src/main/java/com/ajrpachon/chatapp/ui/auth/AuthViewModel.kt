package com.ajrpachon.chatapp.ui.auth
import com.ajrpachon.chatapp.utils.catchResult

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.IntegrityChecker
import com.ajrpachon.chatapp.utils.IntegrityResult
import com.ajrpachon.chatapp.utils.SessionGuard
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(
    private val application: Application,
    private val supabase: SupabaseClient,
    private val setUsernameUseCase: SetUsernameUseCase,
    private val googleWebClientId: String,
    private val userDao: UserDao,
    private val fcmTokenManager: FcmTokenManager,
    private val sessionGuard: SessionGuard,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            runIntegrityCheck()

            catchResult {
                val session = supabase.auth.currentSessionOrNull() ?: run {
                    _state.update { it.copy(isLoading = false) }
                    return@catchResult
                }
                val userId = session.user?.id ?: run {
                    _state.update { it.copy(isLoading = false) }
                    return@catchResult
                }
                val profileResult = catchResult {
                    supabase.postgrest["profiles"]
                        .select { filter { eq("id", userId) } }
                        .decodeSingleOrNull<UserDTO>()
                }
                val profile = profileResult.getOrNull()
                when {
                    profile?.username?.isNotBlank() == true -> {
                        val email = session.user?.email ?: ""
                        userDao.clearCurrentUser()
                        userDao.upsert(profile.toDBO(email = email, isCurrentUser = true))
                        launch { catchResult { fcmTokenManager.syncToken() } }
                        _effect.send(AuthEffect.NavigateToHome)
                    }
                    profileResult.isFailure && userDao.getById(userId) != null -> {
                        launch { catchResult { fcmTokenManager.syncToken() } }
                        _effect.send(AuthEffect.NavigateToHome)
                    }
                    else -> _state.update { it.copy(isLoading = false, needsUsername = true) }
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "Session restore failed", e)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SignInWithGoogle -> signInWithGoogle(intent.context)
            is AuthIntent.SignInWithEmail -> signInWithEmail()
            is AuthIntent.SignUpWithEmail -> signUpWithEmail()
            is AuthIntent.ToggleMode -> _state.update { it.copy(authMode = intent.mode, error = null, showRegisterSuggestion = false) }
            is AuthIntent.SwitchToRegister -> _state.update { it.copy(authMode = AuthMode.SIGN_UP, error = null, showRegisterSuggestion = false) }
            is AuthIntent.EmailChanged -> _state.update { it.copy(emailInput = intent.value, error = null) }
            is AuthIntent.PasswordChanged -> _state.update { it.copy(passwordInput = intent.value, error = null) }
            is AuthIntent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPasswordInput = intent.value, error = null) }
            is AuthIntent.UsernameChanged -> _state.update { it.copy(usernameInput = intent.value, usernameError = null) }
            is AuthIntent.ConfirmUsername -> confirmUsername()
            is AuthIntent.SignOut -> signOut()
            is AuthIntent.DismissError -> _state.update { it.copy(error = null) }
            is AuthIntent.DismissEmailVerification -> _state.update { it.copy(showEmailVerification = false) }
            is AuthIntent.MfaCodeChanged -> _state.update { it.copy(mfaCodeInput = intent.value, mfaError = null) }
            is AuthIntent.VerifyMfaCode -> verifyMfaCode()
        }
    }

    // ── Google ─────────────────────────────────────────────────────────────────

    private fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = MessageDigest.getInstance("SHA-256")
                .digest(rawNonce.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val credentialManager = CredentialManager.create(context)

            // 1st attempt: one-tap (fastest, no UI if already authorized)
            val oneTapResult = catchResult {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(googleWebClientId)
                            .setNonce(hashedNonce)
                            .build()
                    )
                    .build()
                credentialManager.getCredential(context, request)
            }

            // 2nd attempt: full account picker (when one-tap can't access existing accounts)
            val credentialResult = if (oneTapResult.isFailure && oneTapResult.exceptionOrNull() is NoCredentialException) {
                catchResult {
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(
                            GetSignInWithGoogleOption.Builder(googleWebClientId)
                                .setNonce(hashedNonce)
                                .build()
                        )
                        .build()
                    credentialManager.getCredential(context, request)
                }
            } else {
                oneTapResult
            }

            credentialResult.onSuccess { result ->
                catchResult {
                    val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    supabase.auth.signInWith(IDToken) {
                        provider = Google
                        this.idToken = googleCredential.idToken
                        nonce = rawNonce
                    }
                    finishSignIn()
                }.onFailure { e ->
                    AppLogger.e(TAG, "Google sign-in supabase failed", e)
                    _state.update { it.copy(error = e.message ?: "Error con Google") }
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "Google sign-in credential failed", e)
                if (e is NoCredentialException) {
                    _effect.send(AuthEffect.OpenAddGoogleAccount)
                } else {
                    _state.update { it.copy(error = e.message ?: "Error con Google") }
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // ── Email/Password ─────────────────────────────────────────────────────────

    private fun signInWithEmail() {
        val email = _state.value.emailInput.trim()
        val password = _state.value.passwordInput
        val validationError = validateEmailPassword(email, password)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            catchResult {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                finishSignIn()
            }.onFailure { e ->
                AppLogger.e(TAG, "Email sign-in failed", e)
                _state.update { it.copy(error = e.toSignInMessage(), showRegisterSuggestion = e.isInvalidCredentials()) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun signUpWithEmail() {
        val email = _state.value.emailInput.trim()
        val password = _state.value.passwordInput
        val confirm = _state.value.confirmPasswordInput
        val validationError = validateEmailPassword(email, password)
            ?: if (password.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
            ?: if (password != confirm) "Las contraseñas no coinciden" else null
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val signUpResult = catchResult {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
            signUpResult.onFailure { e ->
                AppLogger.e(TAG, "Email sign-up failed", e)
                _state.update { it.copy(error = e.toSignUpMessage(), isLoading = false) }
                return@launch
            }
            val hasSession = supabase.auth.currentSessionOrNull() != null
            if (hasSession) {
                catchResult { finishSignIn() }.onFailure { e ->
                    AppLogger.e(TAG, "Post sign-up finishSignIn failed", e)
                    _state.update { it.copy(error = e.message ?: "Error al completar el registro") }
                }
            } else {
                _state.update { it.copy(showEmailVerification = true) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun validateEmailPassword(email: String, password: String): String? = when {
        email.isBlank() || password.isBlank() -> "Introduce tu correo y contraseña"
        else -> null
    }

    private fun Throwable.isInvalidCredentials(): Boolean =
        isSupabaseErrorCode("invalid_credentials") ||
                message?.contains("Invalid login", ignoreCase = true) == true

    private fun Throwable.toSignInMessage(): String = when {
        isInvalidCredentials() -> "Correo o contraseña incorrectos"
        isSupabaseErrorCode("email_not_confirmed") ||
                message?.contains("Email not confirmed", ignoreCase = true) == true ->
            "Verifica tu correo antes de iniciar sesión"
        else -> message ?: "Error al iniciar sesión"
    }

    private fun Throwable.toSignUpMessage(): String = when {
        isSupabaseErrorCode("user_already_exists") ||
                message?.contains("already registered", ignoreCase = true) == true ||
                message?.contains("already been registered", ignoreCase = true) == true ->
            "Este correo ya está registrado. Inicia sesión en su lugar."
        else -> message ?: "Error al registrarse"
    }

    private fun Throwable.isSupabaseErrorCode(code: String): Boolean {
        val restEx = this as? io.github.jan.supabase.exceptions.RestException ?: return false
        return restEx.error.equals(code, ignoreCase = true)
    }

    // ── Common post-auth flow ──────────────────────────────────────────────────

    private suspend fun finishSignIn() {
        val session = supabase.auth.currentSessionOrNull()
        val userId = session?.user?.id ?: error("No user after sign-in")
        val profile = supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserDTO>()

        if (profile?.username?.isNotBlank() == true) {
            val email = session.user?.email ?: ""
            userDao.clearCurrentUser()
            userDao.upsert(profile.toDBO(email = email, isCurrentUser = true))
            catchResult { fcmTokenManager.syncToken() }
            sessionGuard.recordActivity()

            // Check if MFA challenge is required (user has a verified TOTP factor but AAL1 session)
            val mfaResult = catchResult { supabase.auth.mfa.getAuthenticatorAssuranceLevel() }
            val aal = mfaResult.getOrNull()
            if (aal != null && aal.currentLevel != aal.nextLevel) {
                val factorsResult = catchResult { supabase.auth.mfa.listFactors() }
                val totpFactor = factorsResult.getOrNull()?.totp
                    ?.firstOrNull { it.status.name.equals("verified", ignoreCase = true) }
                if (totpFactor != null) {
                    _state.update { it.copy(
                        needsMfaChallenge = true,
                        mfaFactorId = totpFactor.id,
                        isLoading = false,
                    ) }
                    return
                }
            }

            _effect.send(AuthEffect.NavigateToHome)
        } else {
            _state.update { it.copy(needsUsername = true) }
        }
    }

    // ── MFA Challenge ──────────────────────────────────────────────────────────

    private fun verifyMfaCode() {
        val factorId = _state.value.mfaFactorId ?: return
        val code = _state.value.mfaCodeInput.trim()
        if (code.length != 6) {
            _state.update { it.copy(mfaError = "Introduce los 6 dígitos del código") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(mfaIsLoading = true, mfaError = null) }
            catchResult {
                val challenge = supabase.auth.mfa.createChallenge(factorId)
                supabase.auth.mfa.verifyTotp(factorId = factorId, challengeId = challenge.id, code = code)
                _state.update { it.copy(needsMfaChallenge = false, mfaCodeInput = "", mfaIsLoading = false) }
                finishSignIn()
            }.onFailure { e ->
                AppLogger.e(TAG, "verifyMfaCode failed", e)
                _state.update { it.copy(
                    mfaIsLoading = false,
                    mfaError = "Código incorrecto. Intenta de nuevo.",
                ) }
            }
        }
    }

    // ── Username ───────────────────────────────────────────────────────────────

    private fun confirmUsername() {
        val username = _state.value.usernameInput.trim()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, usernameError = null) }
            catchResult {
                val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
                setUsernameUseCase(userId, username)
                    .onSuccess {
                        _state.update { it.copy(needsUsername = false) }
                        launch { catchResult { fcmTokenManager.syncToken() } }
                        sessionGuard.recordActivity()
                        _effect.send(AuthEffect.NavigateToHome)
                    }
                    .onFailure { e ->
                        _state.update { it.copy(usernameError = e.message) }
                    }
            }.onFailure { e ->
                _state.update { it.copy(usernameError = e.message ?: "Error inesperado") }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            supabase.auth.signOut()
            sessionGuard.clearSession()
            _state.update { AuthState() }
        }
    }

    private suspend fun runIntegrityCheck() {
        when (val result = IntegrityChecker.check(application, supabase)) {
            is IntegrityResult.Passed -> AppLogger.d(TAG, "Integrity check passed")
            is IntegrityResult.Failed -> {
                AppLogger.w(TAG, "Integrity check failed: ${result.reason}")
                _effect.send(AuthEffect.IntegrityFailed(result.reason))
            }
            is IntegrityResult.Error -> {
                // Network/Play Store unavailable — allow through but log
                AppLogger.w(TAG, "Integrity check error (non-blocking): ${result.message}")
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
