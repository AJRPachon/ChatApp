package com.ajrpachon.chatapp.ui.call
import com.ajrpachon.chatapp.utils.catchResult

import android.app.NotificationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.service.FcmMessageHandler
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncomingCallViewModel(
    private val notificationManager: NotificationManager,
    private val callRepository: CallRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(IncomingCallState())
    val state = _state.asStateFlow()

    private var statusJob: Job? = null

    init {
        AppLogger.d(TAG, "init: vmHash=${System.identityHashCode(this)} starting observeIncomingCalls")
        // Observe every state emission to verify StateFlow delivers to collectors
        viewModelScope.launch {
            _state.collect { s ->
                AppLogger.d(TAG, "STATE EMIT vmHash=${System.identityHashCode(this@IncomingCallViewModel)}: incomingCall=${s.incomingCall?.id ?: "null"}")
            }
        }
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                AppLogger.d(TAG, "init: userId=${user.id} listening for incoming calls")
                callRepository.observeIncomingCalls(user.id).collect { call ->
                    AppLogger.d(TAG, "init: INCOMING CALL callId=${call.id} caller=${call.callerName} room=${call.roomName}")
                    _state.update { IncomingCallState(incomingCall = call) }
                    observeCallerCancellation(call.id)
                }
            }.onFailure { e -> AppLogger.e(TAG, "Observe incoming calls failed", e) }
        }
    }

    private fun observeCallerCancellation(callId: String) {
        AppLogger.d(TAG, "observeCallerCancellation: callId=$callId")
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            // Primary: fast INSERT-based signal
            launch {
                catchResult {
                    AppLogger.d(TAG, "observeCallerCancellation: listening hangup signal callId=$callId")
                    callRepository.observeHangupSignal(callId).collect {
                        AppLogger.d(TAG, "observeCallerCancellation: HANGUP SIGNAL received callId=$callId")
                        dismissIfCurrent(callId)
                    }
                }.onFailure { e -> AppLogger.e(TAG, "observeHangupSignal failed", e) }
            }
            // Fallback: DB status change
            catchResult {
                AppLogger.d(TAG, "observeCallerCancellation: listening status changes callId=$callId")
                callRepository.observeCallStatus(callId).collect { status ->
                    AppLogger.d(TAG, "observeCallerCancellation: status=$status callId=$callId")
                    if (status == "ended" || status == "rejected") dismissIfCurrent(callId)
                }
            }.onFailure { e -> AppLogger.e(TAG, "observeCallStatus failed", e) }
        }
    }

    private fun dismissIfCurrent(callId: String) {
        val current = _state.value.incomingCall?.id
        AppLogger.d(TAG, "dismissIfCurrent: callId=$callId currentCallId=$current")
        statusJob?.cancel()
        _state.update { state ->
            if (state.incomingCall?.id == callId) {
                AppLogger.d(TAG, "dismissIfCurrent: dismissed")
                IncomingCallState(incomingCall = null)
            } else {
                AppLogger.d(TAG, "dismissIfCurrent: skipped (different call)")
                state
            }
        }
    }

    fun dismiss() {
        AppLogger.d(TAG, "dismiss: called")
        statusJob?.cancel()
        _state.update { IncomingCallState(incomingCall = null) }
        cancelCallNotification()
    }

    fun reject(callId: String) {
        AppLogger.d(TAG, "reject: callId=$callId")
        cancelCallNotification()
        viewModelScope.launch {
            callRepository.rejectCall(callId)
            dismissIfCurrent(callId)
        }
    }

    private fun cancelCallNotification() {
        notificationManager.cancel(FcmMessageHandler.CALL_NOTIF_ID)
    }

    companion object {
        private const val TAG = "IncomingCallViewModel"
    }
}
