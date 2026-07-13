package com.ajrpachon.chatapp.ui.profile
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.AppLockRepository
import com.ajrpachon.chatapp.data.local.ThemeRepository
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.UploadLimits.checkAvatarSize
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qrcode.QRCode

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val fcmTokenManager: FcmTokenManager,
    private val userRepository: UserRepository,
    private val themeRepository: ThemeRepository,
    private val appLockRepository: AppLockRepository,
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
                updateState { it.copy(error = e.message ?: "Error al subir la foto") }
            }
            updateState { it.copy(isUploadingAvatar = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            catchResult {
                fcmTokenManager.deleteToken()
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
                fcmTokenManager.deleteToken()
                authRepository.signOutAll()
                userRepository.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out all failed", e) }
            sendEffect(ProfileEffect.NavigateToAuth)
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
                    enrollError = e.message ?: "Error al iniciar verificacion en dos pasos",
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
            }.onFailure { e ->
                AppLogger.e(TAG, "verify2FACode failed", e)
                updateState { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    verifyError = e.message ?: "Codigo incorrecto. Intenta de nuevo.",
                )) }
            }
        }
    }

    private fun toggleAppLock() {
        viewModelScope.launch {
            catchResult {
                if (state.value.isAppLockEnabled) appLockRepository.disable()
                else appLockRepository.enable()
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
                    enrollError = e.message ?: "Error al desactivar la verificacion en dos pasos",
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
                        error = e.message ?: "Error al guardar el nombre",
                    ) }
                }
        }
    }

    private suspend fun generateQrBitmap(userId: String) {
        val bitmap = withContext(Dispatchers.Default) {
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
