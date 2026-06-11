package com.ajrpachon.chatapp.ui.userinfo

data class UserInfoState(
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = true,
)
