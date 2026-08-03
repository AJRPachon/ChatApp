package com.ajrpachon.chatapp.ui.call

import com.ajrpachon.chatapp.domain.model.CallBO

data class IncomingCallState(val incomingCall: CallBO? = null)

sealed interface IncomingCallEffect

sealed interface IncomingCallIntent {
    data object Dismiss : IncomingCallIntent
    data class Reject(val callId: String) : IncomingCallIntent
    data class Accept(val callId: String) : IncomingCallIntent
}
