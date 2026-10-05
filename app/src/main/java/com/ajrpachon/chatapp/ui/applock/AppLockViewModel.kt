package com.ajrpachon.chatapp.ui.applock

import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.ui.common.UiText

class AppLockViewModel : BaseViewModel<AppLockState, AppLockEffect>(AppLockState()) {

    fun onIntent(intent: AppLockIntent) {
        when (intent) {
            is AppLockIntent.AuthSucceeded -> {
                updateState { it.copy(errorMessage = null) }
                sendEffect(AppLockEffect.Authenticated)
            }
            is AppLockIntent.AuthError -> {
                updateState { it.copy(errorMessage = UiText.Dynamic(intent.message)) }
            }
            is AppLockIntent.AuthFailed -> {
                updateState { it.copy(errorMessage = UiText.StringResource(R.string.applock_auth_failed)) }
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
