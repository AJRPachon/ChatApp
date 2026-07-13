package com.ajrpachon.chatapp.ui.userinfo

sealed interface UserInfoEffect

data class UserInfoState(
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = true,
    val mediaUrls: List<String> = emptyList(),
)
