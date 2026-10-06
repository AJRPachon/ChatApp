package com.ajrpachon.chatapp.ui.profile
import com.ajrpachon.chatapp.domain.util.catchResult

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.AuthErrorKind
import com.ajrpachon.chatapp.domain.model.AuthException
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AppLockRepository
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.ui.common.toUiText
import com.ajrpachon.chatapp.domain.repository.AnalyticsEvents
import com.ajrpachon.chatapp.utils.AppDispatchers
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.UploadLimits.checkAvatarSize
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qrcode.QRCode

// LongParameterList: constructor injection via Koin, one parameter per distinct collaborator.
@Suppress("LongParameterList")
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val fcmTokenRepository: FcmTokenRepository,
    private val userRepository: UserRepository,
    private val themeRepository: ThemeRepository,
    private val appLockRepository: AppLockRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val dispatchers: AppDispatchers,
) : BaseViewModel<ProfileState, ProfileEffect>(ProfileState()) {

    init {
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                updateState {
                    it.copy(
                        userId = user.id,
                        displayName = user.displayName,
                        editingDisplayName = user.displayName,
                        username = user.username,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        showOnlineStatus = user.showOnlineStatus,
                    )
                }
                generateQrBitmap(user.id)
            }.onFailure { e -> AppLogger.e(TAG, "Load profile failed", e) }
            load2FAStatus()
        }
        themeRepository.observe()
            .onEach { pref -> updateState { it.copy(themePreference = pref) } }
            .launchIn(viewModelScope)
        appLockRepository.isEnabled
            .onEach { enabled -> updateState { it.copy(isAppLockEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.ToggleOnlineStatus -> {
                val userId = authRepository.getCurrentUserId() ?: return
                updateState { it.copy(showOnlineStatus = intent.show) }
                viewModelScope.launch {
                    catchResult { userRepository.updateShowOnlineStatus(userId, intent.show) }
                        .onFailure { e ->
                            AppLogger.e(TAG, "updateShowOnlineStatus failed", e)
                            updateState { it.copy(showOnlineStatus = !intent.show) }
                        }
                }
            }
            is ProfileIntent.SetTheme -> {
                viewModelScope.launch {
                    catchResult { themeRepository.set(intent.theme) }
                        .onFailure { e -> AppLogger.e(TAG, "SetTheme failed", e) }
                }
            }
            is ProfileIntent.Enroll2FA -> enroll2FA()
            is ProfileIntent.Verify2FACode -> verify2FACode(intent.code)
            is ProfileIntent.Disable2FA -> disable2FA()
            is ProfileIntent.Dismiss2FASheet -> updateState {
                it.copy(twoFactor = it.twoFactor.copy(
                    showEnrollSheet = false,
                    qrCodeSvg = null,
                    secret = null,
                    enrollError = null,
                    verifyError = null,
                ))
            }
            is ProfileIntent.ToggleAppLock -> toggleAppLock()
            is ProfileIntent.EditDisplayName -> updateState { it.copy(editingDisplayName = intent.value) }
            is ProfileIntent.SaveDisplayName -> saveDisplayName()
        }
    }

    fun onAvatarSelected(bytes: ByteArray, mimeType: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isUploadingAvatar = true, error = null) }
            catchResult {
                bytes.checkAvatarSize()
                val url = userRepository.uploadAvatar(userId, bytes, mimeType)
                updateState { it.copy(avatarUrl = url) }
            }.onFailure { e ->
                AppLogger.e(TAG, "Avatar upload failed", e)
                updateState { it.copy(error = e.toUiText(R.string.profile_error_upload_photo)) }
            }
            updateState { it.copy(isUploadingAvatar = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            catchResult {
                fcmTokenRepository.deleteToken()
                authRepository.signOut()
                userRepository.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out failed", e) }
            sendEffect(ProfileEffect.NavigateToAuth)
        }
    }

    fun requestSignOutAll() {
        viewModelScope.launch { sendEffect(ProfileEffect.ShowSignOutAllConfirm) }
    }

    fun signOutAll() {
        viewModelScope.launch {
            catchResult {
                fcmTokenRepository.deleteToken()
                authRepository.signOutAll()
                userRepository.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out all failed", e) }
            sendEffect(ProfileEffect.NavigateToAuth)
        }
    }

    fun requestDeleteAccount() {
        viewModelScope.launch { sendEffect(ProfileEffect.ShowDeleteAccountConfirm) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            updateState { it.copy(isDeletingAccount = true, error = null) }
            catchResult {
                fcmTokenRepository.deleteToken()
                authRepository.deleteAccount()
                userRepository.clearCurrentUser()
            }.onSuccess {
                updateState { it.copy(isDeletingAccount = false) }
                sendEffect(ProfileEffect.NavigateToAuth)
            }.onFailure { e ->
                AppLogger.e(TAG, "Delete account failed", e)
                updateState { it.copy(
                    isDeletingAccount = false,
                    error = e.toDeleteAccountErrorMessage(),
                ) }
            }
        }
    }

    private fun Throwable.toDeleteAccountErrorMessage(): UiText {
        return when ((this as? AuthException)?.kind) {
            AuthErrorKind.SESSION_EXPIRED -> UiText.StringResource(R.string.profile_error_session_invalid)
            AuthErrorKind.TOO_MANY_REQUESTS -> UiText.StringResource(R.string.profile_error_too_many_attempts)
            AuthErrorKind.SERVER_ERROR -> UiText.StringResource(R.string.profile_error_delete_server)
            else -> toUiText(R.string.profile_error_delete_account)
        }
    }

    private suspend fun load2FAStatus() {
        catchResult {
            val totpFactorId = authRepository.getVerifiedTotpFactorId()
            updateState { it.copy(twoFactor = it.twoFactor.copy(
                isEnrolled = totpFactorId != null,
                factorId = totpFactorId,
            )) }
        }.onFailure { e -> AppLogger.e(TAG, "load2FAStatus failed", e) }
    }

    private fun enroll2FA() {
        viewModelScope.launch {
            updateState { it.copy(twoFactor = it.twoFactor.copy(isLoading = true, enrollError = null)) }
            catchResult {
                val enrollment = authRepository.enrollTotp()
                updateState { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    showEnrollSheet = true,
                    qrCodeSvg = enrollment.qrCodeSvg,
                    secret = enrollment.secret,
                    factorId = enrollment.factorId,
                )) }
            }.onFailure { e ->
                AppLogger.e(TAG, "enroll2FA failed", e)
                updateState { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    enrollError = e.toUiText(R.string.profile_error_2fa_start),
                )) }
            }
        }
    }

    private fun verify2FACode(code: String) {
        val factorId = state.value.twoFactor.factorId ?: return
        viewModelScope.launch {
            updateState { it.copy(twoFactor = it.twoFactor.copy(isLoading = true, verifyError = null)) }
            catchResult {
                val challengeId = authRepository.createMfaChallenge(factorId)
                authRepository.verifyMfaChallenge(factorId, challengeId, code)
                updateState { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    isEnrolled = true,
                    showEnrollSheet = false,
                    qrCodeSvg = null,
                    secret = null,
                )) }
                analyticsTracker.logEvent(AnalyticsEvents.MFA_ENROLLED)
            }.onFailure { e ->
                AppLogger.e(TAG, "verify2FACode failed", e)
                updateState { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    verifyError = e.toUiText(R.string.profile_error_2fa_code),
                )) }
            }
        }
    }

    private fun toggleAppLock() {
        viewModelScope.launch {
            catchResult {
                if (state.value.isAppLockEnabled) {
                    appLockRepository.disable()
                } else if (!appLockRepository.canUseDeviceCredential()) {
                    // Refuse to enable if the device has no biometric/PIN enrolled —
                    // AppLockScreen's biometric prompt never auto-launches in that case,
                    // and (since the back-press bypass was fixed) there is no other
                    // UI-driven way back in. Enabling anyway would strand the user.
                    sendEffect(ProfileEffect.AppLockCredentialMissing)
                } else {
                    appLockRepository.enable()
                }
            }.onFailure { e -> AppLogger.e(TAG, "toggleAppLock failed", e) }
        }
    }

    private fun disable2FA() {
        val factorId = state.value.twoFactor.factorId ?: return
        viewModelScope.launch {
            updateState { it.copy(twoFactor = it.twoFactor.copy(isLoading = true)) }
            catchResult {
                authRepository.unenrollFactor(factorId)
                updateState { it.copy(twoFactor = TwoFactorState(isEnrolled = false)) }
            }.onFailure { e ->
                AppLogger.e(TAG, "disable2FA failed", e)
                updateState { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    enrollError = e.toUiText(R.string.profile_error_2fa_disable),
                )) }
            }
        }
    }

    private fun saveDisplayName() {
        val userId = authRepository.getCurrentUserId() ?: return
        val newName = state.value.editingDisplayName.trim()
        if (newName.isBlank() || newName == state.value.displayName) return
        viewModelScope.launch {
            updateState { it.copy(isSavingDisplayName = true, error = null) }
            catchResult { userRepository.updateDisplayName(userId, newName) }
                .onSuccess { updateState { it.copy(displayName = newName, isSavingDisplayName = false) } }
                .onFailure { e ->
                    AppLogger.e(TAG, "updateDisplayName failed", e)
                    updateState { it.copy(
                        isSavingDisplayName = false,
                        editingDisplayName = it.displayName,
                        error = e.toUiText(R.string.profile_error_save_name),
                    ) }
                }
        }
    }

    private suspend fun generateQrBitmap(userId: String) {
        val bitmap = withContext(dispatchers.default) {
            runCatching {
                val content = "chatapp://user/$userId"
                val rendered = QRCode(content).render()
                rendered.nativeImage() as android.graphics.Bitmap
            }.getOrNull()
        }
        updateState { it.copy(qrBitmap = bitmap) }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
