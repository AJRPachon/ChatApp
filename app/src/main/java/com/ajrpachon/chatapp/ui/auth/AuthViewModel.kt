package com.ajrpachon.chatapp.ui.auth

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.AuthErrorKind
import com.ajrpachon.chatapp.domain.model.AuthException
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.domain.model.IntegrityResultBO
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.ui.common.toUiText
import com.ajrpachon.chatapp.utils.SessionGuard
import com.ajrpachon.chatapp.domain.util.catchResult
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val setUsernameUseCase: SetUsernameUseCase,
    private val fcmTokenRepository: FcmTokenRepository,
    private val sessionGuard: SessionGuard,
) : BaseViewModel<AuthState, AuthEffect>(AuthState()) {

    init {
        viewModelScope.launch {
            runIntegrityCheck()
            catchResult {
                val session = authRepository.getCurrentSessionInfo() ?: run {
                    updateState { it.copy(isLoading = false) }
                    return@catchResult
                }
                val userId = session.userId
                val profileResult = catchResult { userRepository.fetchProfileFromRemote(userId) }
                val profile = profileResult.getOrNull()
                when {
                    profile?.username?.isNotBlank() == true -> {
                        userRepository.markAsCurrentUser(userId, session.email ?: "")
                        launch { catchResult { fcmTokenRepository.syncToken() } }
                        sendEffect(AuthEffect.NavigateToHome)
                    }
                    profileResult.isFailure && userRepository.getUserById(userId) != null -> {
                        launch { catchResult { fcmTokenRepository.syncToken() } }
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
            is AuthIntent.SignInWithGoogle -> startGoogleSignIn()
            is AuthIntent.GoogleTokenReceived -> completeGoogleSignIn(intent.idToken)
            is AuthIntent.GoogleSignInFailed -> failGoogleSignIn(intent.message, intent.noCredential)
            is AuthIntent.GoogleSignInCancelled -> cancelGoogleSignIn()
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

    // The raw nonce of the Google sign-in in flight. Its SHA-256 goes to Credential Manager (via the
    // screen) and the raw value goes to Supabase with the token, which lets it check they match.
    private var pendingGoogleNonce: String? = null

    private fun startGoogleSignIn() {
        updateState { it.copy(isLoading = true, error = null) }
        val rawNonce = UUID.randomUUID().toString()
        pendingGoogleNonce = rawNonce
        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }
        sendEffect(AuthEffect.RequestGoogleCredential(hashedNonce))
    }

    private fun completeGoogleSignIn(idToken: String) {
        val rawNonce = pendingGoogleNonce
        pendingGoogleNonce = null
        if (rawNonce == null) {
            updateState { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            catchResult {
                authRepository.signInWithGoogle(idToken, rawNonce)
                finishSignIn()
            }.onFailure { e ->
                AppLogger.e(TAG, "Google sign-in supabase failed", e)
                updateState { it.copy(error = e.toUiText(R.string.auth_error_google)) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun failGoogleSignIn(message: String?, noCredential: Boolean) {
        pendingGoogleNonce = null
        AppLogger.e(TAG, "Google sign-in credential failed: $message")
        if (noCredential) sendEffect(AuthEffect.OpenAddGoogleAccount)
        else updateState { it.copy(error = message?.let(UiText::Dynamic) ?: UiText.StringResource(R.string.auth_error_google)) }
        updateState { it.copy(isLoading = false) }
    }

    private fun cancelGoogleSignIn() {
        pendingGoogleNonce = null
        updateState { it.copy(isLoading = false) }
    }

    private fun signInWithEmail() {
        val email = state.value.emailInput.trim()
        val password = state.value.passwordInput
        val validationError = validateEmailPassword(email, password)
        if (validationError != null) { updateState { it.copy(error = validationError) }; return }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            catchResult {
                authRepository.signInWithEmail(email, password)
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
            ?: if (password.length < MIN_PASSWORD_LENGTH) UiText.StringResource(R.string.auth_error_password_short) else null
            ?: if (password != confirm) UiText.StringResource(R.string.auth_error_passwords_mismatch) else null
        if (validationError != null) { updateState { it.copy(error = validationError) }; return }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            val signUpResult = catchResult { authRepository.signUpWithEmail(email, password) }
            signUpResult.onFailure { e ->
                AppLogger.e(TAG, "Email sign-up failed", e)
                updateState { it.copy(error = e.toSignUpMessage(), isLoading = false) }
                return@launch
            }
            val hasSession = signUpResult.getOrDefault(false)
            if (hasSession) {
                catchResult { finishSignIn() }.onFailure { e ->
                    AppLogger.e(TAG, "Post sign-up finishSignIn failed", e)
                    updateState { it.copy(error = e.toUiText(R.string.auth_error_complete_sign_up)) }
                }
            } else {
                updateState { it.copy(showEmailVerification = true) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun validateEmailPassword(email: String, password: String): UiText? = when {
        email.isBlank() || password.isBlank() -> UiText.StringResource(R.string.auth_error_credentials_required)
        else -> null
    }

    private fun Throwable.authKind(): AuthErrorKind? = (this as? AuthException)?.kind

    private fun Throwable.isInvalidCredentials(): Boolean = authKind() == AuthErrorKind.INVALID_CREDENTIALS

    private fun Throwable.toSignInMessage(): UiText = when (authKind()) {
        AuthErrorKind.INVALID_CREDENTIALS -> UiText.StringResource(R.string.auth_error_invalid_credentials)
        AuthErrorKind.EMAIL_NOT_CONFIRMED -> UiText.StringResource(R.string.auth_error_email_not_confirmed)
        else -> toUiText(R.string.auth_error_sign_in)
    }

    private fun Throwable.toSignUpMessage(): UiText = when (authKind()) {
        AuthErrorKind.EMAIL_ALREADY_REGISTERED -> UiText.StringResource(R.string.auth_error_email_registered)
        else -> toUiText(R.string.auth_error_sign_up)
    }

    private suspend fun finishSignIn() {
        val session = authRepository.getCurrentSessionInfo() ?: error("No user after sign-in")
        val userId = session.userId
        val profile = userRepository.fetchProfileFromRemote(userId)
        if (profile?.username?.isNotBlank() == true) {
            userRepository.markAsCurrentUser(userId, session.email ?: "")
            catchResult { fcmTokenRepository.syncToken() }
            sessionGuard.recordActivity()
            val aal = catchResult { authRepository.getMfaAssuranceLevel() }.getOrNull()
            if (aal != null && aal.current != aal.next) {
                val totpFactorId = catchResult { authRepository.getVerifiedTotpFactorId() }.getOrNull()
                if (totpFactorId != null) {
                    updateState { it.copy(needsMfaChallenge = true, mfaFactorId = totpFactorId, isLoading = false) }
                    return
                }
            }
            sendEffect(AuthEffect.NavigateToHome)
        } else {
            updateState { it.copy(needsUsername = true) }
        }
    }

    private fun verifyMfaCode() {
        val factorId = state.value.mfaFactorId ?: return
        val code = state.value.mfaCodeInput.trim()
        if (code.length != 6) { updateState { it.copy(mfaError = UiText.StringResource(R.string.auth_error_mfa_code_length)) }; return }
        viewModelScope.launch {
            updateState { it.copy(mfaIsLoading = true, mfaError = null) }
            catchResult {
                val challengeId = authRepository.createMfaChallenge(factorId)
                authRepository.verifyMfaChallenge(factorId, challengeId, code)
                updateState { it.copy(needsMfaChallenge = false, mfaCodeInput = "", mfaIsLoading = false) }
                finishSignIn()
            }.onFailure { e ->
                AppLogger.e(TAG, "verifyMfaCode failed", e)
                updateState { it.copy(mfaIsLoading = false, mfaError = UiText.StringResource(R.string.auth_error_mfa_code)) }
            }
        }
    }

    private fun confirmUsername() {
        val username = state.value.usernameInput.trim()
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, usernameError = null) }
            catchResult {
                val userId = authRepository.getCurrentUserId() ?: error("Not authenticated")
                setUsernameUseCase(userId, username)
                    .onSuccess {
                        updateState { it.copy(needsUsername = false) }
                        launch { catchResult { fcmTokenRepository.syncToken() } }
                        sessionGuard.recordActivity()
                        sendEffect(AuthEffect.NavigateToHome)
                    }
                    .onFailure { e -> updateState { it.copy(usernameError = e.toUiText()) } }
            }.onFailure { e ->
                updateState { it.copy(usernameError = e.toUiText(R.string.auth_error_unexpected)) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            catchResult { authRepository.signOut() }
            sessionGuard.clearSession()
            updateState { AuthState() }
        }
    }

    private suspend fun runIntegrityCheck() {
        when (val result = authRepository.checkIntegrity()) {
            is IntegrityResultBO.Passed -> AppLogger.d(TAG, "Integrity check passed")
            is IntegrityResultBO.Failed -> {
                AppLogger.w(TAG, "Integrity check failed: ${result.reason}")
                sendEffect(AuthEffect.IntegrityFailed(result.reason))
            }
            is IntegrityResultBO.Error -> {
                AppLogger.w(TAG, "Integrity check error (non-blocking): ${result.message}")
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
