package com.ajrpachon.chatapp.ui.applock

import com.ajrpachon.chatapp.ui.common.UiText

data class AppLockState(
    val errorMessage: UiText? = null,
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
