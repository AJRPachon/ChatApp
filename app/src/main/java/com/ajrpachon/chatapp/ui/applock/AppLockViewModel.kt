package com.ajrpachon.chatapp.ui.applock

import com.ajrpachon.chatapp.ui.common.BaseViewModel

class AppLockViewModel : BaseViewModel<AppLockState, AppLockEffect>(AppLockState()) {

    fun onIntent(intent: AppLockIntent) {
        when (intent) {
            is AppLockIntent.AuthSucceeded -> {
                updateState { it.copy(errorMessage = null) }
                sendEffect(AppLockEffect.Authenticated)
            }
            is AppLockIntent.AuthError -> {
                updateState { it.copy(errorMessage = intent.message) }
            }
            is AppLockIntent.AuthFailed -> {
                updateState { it.copy(errorMessage = "Autenticación fallida. Inténtalo de nuevo.") }
            }
            is AppLockIntent.ClearError -> {
                updateState { it.copy(errorMessage = null) }
            }
        }
    }

    fun requestBiometric() {
        sendEffect(AppLockEffect.LaunchBiometric)
    }
}
