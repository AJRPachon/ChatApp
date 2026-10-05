package com.ajrpachon.chatapp.ui.profile

import android.graphics.Bitmap
import com.ajrpachon.chatapp.domain.model.ThemePreference
import com.ajrpachon.chatapp.ui.common.UiText

data class TwoFactorState(
    val isEnrolled: Boolean = false,
    val isLoading: Boolean = false,
    val showEnrollSheet: Boolean = false,
    val qrCodeSvg: String? = null,
    val secret: String? = null,
    val factorId: String? = null,
    val enrollError: UiText? = null,
    val verifyError: UiText? = null,
)

data class ProfileState(
    val userId: String = "",
    val displayName: String = "",
    val editingDisplayName: String = "",
    val isSavingDisplayName: Boolean = false,
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isUploadingAvatar: Boolean = false,
    val showOnlineStatus: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val twoFactor: TwoFactorState = TwoFactorState(),
    val isAppLockEnabled: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val isDeletingAccount: Boolean = false,
    val error: UiText? = null,
)

sealed interface ProfileIntent {
    data class ToggleOnlineStatus(val show: Boolean) : ProfileIntent
    data class SetTheme(val theme: ThemePreference) : ProfileIntent
    data object Enroll2FA : ProfileIntent
    data class Verify2FACode(val code: String) : ProfileIntent
    data object Disable2FA : ProfileIntent
    data object Dismiss2FASheet : ProfileIntent
    data object ToggleAppLock : ProfileIntent
    data class EditDisplayName(val value: String) : ProfileIntent
    data object SaveDisplayName : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
    data object ShowSignOutAllConfirm : ProfileEffect
    data object ShowDeleteAccountConfirm : ProfileEffect
    data object AppLockCredentialMissing : ProfileEffect
}
