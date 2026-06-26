package com.ajrpachon.chatapp.ui.profile

data class ProfileState(
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isUploadingAvatar: Boolean = false,
    val showOnlineStatus: Boolean = true,
    val error: String? = null,
)

sealed interface ProfileIntent {
    data class ToggleOnlineStatus(val show: Boolean) : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
    data object ShowSignOutAllConfirm : ProfileEffect
}
