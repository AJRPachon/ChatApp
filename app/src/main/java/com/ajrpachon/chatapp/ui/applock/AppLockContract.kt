package com.ajrpachon.chatapp.ui.applock

data class AppLockState(
    val errorMessage: String? = null,
)

sealed interface AppLockIntent {
    data object AuthSucceeded : AppLockIntent
    data class AuthError(val message: String) : AppLockIntent
    data object AuthFailed : AppLockIntent
    data object ClearError : AppLockIntent
}

sealed interface AppLockEffect {
    data object LaunchBiometric : AppLockEffect
    data object Authenticated : AppLockEffect
}
