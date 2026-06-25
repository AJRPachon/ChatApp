package com.ajrpachon.chatapp.ui.profile

data class ProfileState(
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
)

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
    data object ShowSignOutAllConfirm : ProfileEffect
}
