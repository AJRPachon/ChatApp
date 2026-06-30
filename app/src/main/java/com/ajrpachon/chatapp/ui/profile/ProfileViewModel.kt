package com.ajrpachon.chatapp.ui.profile
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.ThemeRepository
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.utils.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.mfa
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ajrpachon.chatapp.utils.UploadLimits.checkAvatarSize
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val AVATARS_BUCKET = "avatars"

class ProfileViewModel(
    private val supabase: SupabaseClient,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val userDao: UserDao,
    private val fcmTokenManager: FcmTokenManager,
    private val userRepository: UserRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                _state.update {
                    it.copy(
                        userId = user.id,
                        displayName = user.displayName,
                        username = user.username,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        showOnlineStatus = user.showOnlineStatus,
                    )
                }
            }.onFailure { e -> AppLogger.e(TAG, "Load profile failed", e) }
            load2FAStatus()
        }
        themeRepository.observe()
            .onEach { pref -> _state.update { it.copy(themePreference = pref) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.ToggleOnlineStatus -> {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return
                _state.update { it.copy(showOnlineStatus = intent.show) }
                viewModelScope.launch {
                    catchResult { userRepository.updateShowOnlineStatus(userId, intent.show) }
                        .onFailure { e ->
                            AppLogger.e(TAG, "updateShowOnlineStatus failed", e)
                            _state.update { it.copy(showOnlineStatus = !intent.show) }
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
            is ProfileIntent.Dismiss2FASheet -> _state.update {
                it.copy(twoFactor = it.twoFactor.copy(
                    showEnrollSheet = false,
                    qrCodeSvg = null,
                    secret = null,
                    enrollError = null,
                    verifyError = null,
                ))
            }
        }
    }

    fun onAvatarSelected(bytes: ByteArray, mimeType: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, error = null) }
            catchResult {
                bytes.checkAvatarSize()
                val ext = if (mimeType.contains("png")) "png" else "jpg"
                val path = "$userId/avatar.$ext"

                supabase.storage[AVATARS_BUCKET].upload(path, bytes) { upsert = true }
                val url = supabase.storage[AVATARS_BUCKET].publicUrl(path)

                supabase.postgrest["profiles"].update(
                    buildJsonObject { put("avatar_url", url) }
                ) { filter { eq("id", userId) } }

                userDao.getById(userId)?.let { userDao.upsert(it.copy(avatarUrl = url)) }
                _state.update { it.copy(avatarUrl = url) }
            }.onFailure { e ->
                AppLogger.e(TAG, "Avatar upload failed", e)
                _state.update { it.copy(error = e.message ?: "Error al subir la foto") }
            }
            _state.update { it.copy(isUploadingAvatar = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            catchResult {
                fcmTokenManager.deleteToken()
                supabase.auth.signOut()
                userDao.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out failed", e) }
            _effect.send(ProfileEffect.NavigateToAuth)
        }
    }

    fun requestSignOutAll() {
        viewModelScope.launch { _effect.send(ProfileEffect.ShowSignOutAllConfirm) }
    }

    fun signOutAll() {
        viewModelScope.launch {
            catchResult {
                fcmTokenManager.deleteToken()
                supabase.auth.signOut(SignOutScope.GLOBAL)
                userDao.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out all failed", e) }
            _effect.send(ProfileEffect.NavigateToAuth)
        }
    }

    private suspend fun load2FAStatus() {
        catchResult {
            val factors = supabase.auth.mfa.listFactors()
            val totpFactor = factors.totp.firstOrNull { it.status.name.equals("verified", ignoreCase = true) }
            _state.update { it.copy(twoFactor = it.twoFactor.copy(
                isEnrolled = totpFactor != null,
                factorId = totpFactor?.id,
            )) }
        }.onFailure { e -> AppLogger.e(TAG, "load2FAStatus failed", e) }
    }

    private fun enroll2FA() {
        viewModelScope.launch {
            _state.update { it.copy(twoFactor = it.twoFactor.copy(isLoading = true, enrollError = null)) }
            catchResult {
                val response = supabase.auth.mfa.enrollTOTP()
                _state.update { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    showEnrollSheet = true,
                    qrCodeSvg = response.qrCode,
                    secret = response.secret,
                    factorId = response.id,
                )) }
            }.onFailure { e ->
                AppLogger.e(TAG, "enroll2FA failed", e)
                _state.update { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    enrollError = e.message ?: "Error al iniciar verificación en dos pasos",
                )) }
            }
        }
    }

    private fun verify2FACode(code: String) {
        val factorId = _state.value.twoFactor.factorId ?: return
        viewModelScope.launch {
            _state.update { it.copy(twoFactor = it.twoFactor.copy(isLoading = true, verifyError = null)) }
            catchResult {
                val challenge = supabase.auth.mfa.createChallenge(factorId)
                supabase.auth.mfa.verifyTotp(factorId = factorId, challengeId = challenge.id, code = code)
                _state.update { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    isEnrolled = true,
                    showEnrollSheet = false,
                    qrCodeSvg = null,
                    secret = null,
                )) }
            }.onFailure { e ->
                AppLogger.e(TAG, "verify2FACode failed", e)
                _state.update { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    verifyError = e.message ?: "Código incorrecto. Intenta de nuevo.",
                )) }
            }
        }
    }

    private fun disable2FA() {
        val factorId = _state.value.twoFactor.factorId ?: return
        viewModelScope.launch {
            _state.update { it.copy(twoFactor = it.twoFactor.copy(isLoading = true)) }
            catchResult {
                supabase.auth.mfa.unenroll(factorId)
                _state.update { it.copy(twoFactor = TwoFactorState(isEnrolled = false)) }
            }.onFailure { e ->
                AppLogger.e(TAG, "disable2FA failed", e)
                _state.update { it.copy(twoFactor = it.twoFactor.copy(
                    isLoading = false,
                    enrollError = e.message ?: "Error al desactivar la verificación en dos pasos",
                )) }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
