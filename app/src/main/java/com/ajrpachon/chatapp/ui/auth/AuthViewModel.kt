package com.ajrpachon.chatapp.ui.auth
import com.ajrpachon.chatapp.utils.catchResult

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.IntegrityChecker
import com.ajrpachon.chatapp.utils.IntegrityResult
import com.ajrpachon.chatapp.utils.SessionGuard
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import androidx.lifecycle.viewModelScope
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
) : BaseViewModel<AuthState, AuthEffect>(AuthState()) {

    init {
        viewModelScope.launch {
            runIntegrityCheck()

            catchResult {
                val session = supabase.auth.currentSessionOrNull() ?: run {
                    updateState { it.copy(isLoading = false) }
                    return@catchResult
                }
                val userId = session.user?.id ?: run {
                    updateState { it.copy(isLoading = false) }
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
                        sendEffect(AuthEffect.NavigateToHome)
                    }
                    profileResult.isFailure && userDao.getById(userId) != null -> {
                        launch { catchResult { fcmTokenManager.syncToken() } }
                        sendEffect(AuthEffect.NavigateToHome)
                    }
                    else -> updateState { it.copy(isLoading = false, needsUsername = true) }
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "Session restore failed", e)
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SignInWithGoogle -> signInWithGoogle(intent.context)
            is AuthIntent.SignInWithEmail -> signInWithEmail()
            is AuthIntent.SignUpWithEmail -> signUpWithEmail()
            is AuthIntent.ToggleMode -> updateState { it.copy(authMode = intent.mode, error = null, showRegisterSuggestion = false) }
            is AuthIntent.SwitchToRegister -> updateState { it.copy(authMode = AuthMode.SIGN_UP, error = null, showRegisterSuggestion = false) }
            is AuthIntent.EmailChanged -> updateState { it.copy(emailInput = intent.value, error = null) }
            is AuthIntent.PasswordChanged -> updateState { it.copy(passwordInput = intent.value, error = null) }
            is AuthIntent.ConfirmPasswordChanged -> updateState { it.copy(confirmPasswordInput = intent.value, error = null) }
            is AuthIntent.UsernameChanged -> updateState { it.copy(usernameInput = intent.value, usernameError = null) }
            is AuthIntent.ConfirmUsername -> confirmUsername()
            is AuthIntent.SignOut -> signOut()
            is AuthIntent.DismissError -> updateState { it.copy(error = null) }
            is AuthIntent.DismissEmailVerification -> updateState { it.copy(showEmailVerification = false) }
            is AuthIntent.MfaCodeChanged -> updateState { it.copy(mfaCodeInput = intent.value, mfaError = null) }
            is AuthIntent.VerifyMfaCode -> verifyMfaCode()
        }
    }

    // ── Google ─────────────────────────────────────────────────────────────────

    private fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
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
                    updateState { it.copy(error = e.message ?: "Error con Google") }
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "Google sign-in credential failed", e)
                if (e is NoCredentialException) {
                    sendEffect(AuthEffect.OpenAddGoogleAccount)
                } else {
                    updateState { it.copy(error = e.message ?: "Error con Google") }
                }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    // ── Email/Password ─────────────────────────────────────────────────────────

    private fun signInWithEmail() {
        val email = state.value.emailInput.trim()
        val password = state.value.passwordInput
        val validationError = validateEmailPassword(email, password)
        if (validationError != null) {
            updateState { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            catchResult {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                finishSignIn()
            }.onFailure { e ->
                AppLogger.e(TAG, "Email sign-in failed", e)
                updateState { it.copy(error = e.toSignInMessage(), showRegisterSuggestion = e.isInvalidCredentials()) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun signUpWithEmail() {
        val email = state.value.emailInput.trim()
        val password = state.value.passwordInput
        val confirm = state.value.confirmPasswordInput
        val validationError = validateEmailPassword(email, password)
            ?: if (password.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
            ?: if (password != confirm) "Las contraseñas no coinciden" else null
        if (validationError != null) {
            updateState { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            val signUpResult = catchResult {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
            signUpResult.onFailure { e ->
                AppLogger.e(TAG, "Email sign-up failed", e)
                updateState { it.copy(error = e.toSignUpMessage(), isLoading = false) }
                return@launch
            }
            val hasSession = supabase.auth.currentSessionOrNull() != null
            if (hasSession) {
                catchResult { finishSignIn() }.onFailure { e ->
                    AppLogger.e(TAG, "Post sign-up finishSignIn failed", e)
                    updateState { it.copy(error = e.message ?: "Error al completar el registro") }
                }
            } else {
                updateState { it.copy(showEmailVerification = true) }
            }
            updateState { it.copy(isLoading = false) }
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
            if (aal != null && aal.current != aal.next) {
                val factorsResult = catchResult { supabase.auth.mfa.retrieveFactorsForCurrentUser() }
                val totpFactor = factorsResult.getOrNull()
                    ?.firstOrNull { it.factorType == "totp" && it.isVerified }
                if (totpFactor != null) {
                    updateState { it.copy(
                        needsMfaChallenge = true,
                        mfaFactorId = totpFactor.id,
                        isLoading = false,
                    ) }
                    return
                }
            }

            sendEffect(AuthEffect.NavigateToHome)
        } else {
            updateState { it.copy(needsUsername = true) }
        }
    }

    // ── MFA Challenge ──────────────────────────────────────────────────────────

    private fun verifyMfaCode() {
        val factorId = state.value.mfaFactorId ?: return
        val code = state.value.mfaCodeInput.trim()
        if (code.length != 6) {
            updateState { it.copy(mfaError = "Introduce los 6 dígitos del código") }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(mfaIsLoading = true, mfaError = null) }
            catchResult {
                val challenge = supabase.auth.mfa.createChallenge(factorId)
                supabase.auth.mfa.verifyChallenge(factorId = factorId, challengeId = challenge.id, code = code)
                updateState { it.copy(needsMfaChallenge = false, mfaCodeInput = "", mfaIsLoading = false) }
                finishSignIn()
            }.onFailure { e ->
                AppLogger.e(TAG, "verifyMfaCode failed", e)
                updateState { it.copy(
                    mfaIsLoading = false,
                    mfaError = "Código incorrecto. Intenta de nuevo.",
                ) }
            }
        }
    }

    // ── Username ───────────────────────────────────────────────────────────────

    private fun confirmUsername() {
        val username = state.value.usernameInput.trim()
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, usernameError = null) }
            catchResult {
                val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
                setUsernameUseCase(userId, username)
                    .onSuccess {
                        updateState { it.copy(needsUsername = false) }
                        launch { catchResult { fcmTokenManager.syncToken() } }
                        sessionGuard.recordActivity()
                        sendEffect(AuthEffect.NavigateToHome)
                    }
                    .onFailure { e ->
                        updateState { it.copy(usernameError = e.message) }
                    }
            }.onFailure { e ->
                updateState { it.copy(usernameError = e.message ?: "Error inesperado") }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            supabase.auth.signOut()
            sessionGuard.clearSession()
            updateState { AuthState() }
        }
    }

    private suspend fun runIntegrityCheck() {
        when (val result = IntegrityChecker.check(application, supabase)) {
            is IntegrityResult.Passed -> AppLogger.d(TAG, "Integrity check passed")
            is IntegrityResult.Failed -> {
                AppLogger.w(TAG, "Integrity check failed: ${result.reason}")
                sendEffect(AuthEffect.IntegrityFailed(result.reason))
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
