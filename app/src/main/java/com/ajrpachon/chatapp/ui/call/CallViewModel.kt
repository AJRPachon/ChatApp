package com.ajrpachon.chatapp.ui.call
import com.ajrpachon.chatapp.utils.catchResult

import android.app.Application
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import io.livekit.android.LiveKit
import io.livekit.android.events.DisconnectReason
import io.livekit.android.events.ParticipantEvent
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private const val MISSED_CALL_TIMEOUT_MS = 20_000L

data class CallArgs(
    val callId: String,
    val conversationId: String,
    val roomName: String,
    val callType: String,
    val isOutgoing: Boolean,
    val isGroup: Boolean,
)

class CallViewModel(
    private val args: CallArgs,
    private val application: Application,
    private val callRepository: CallRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val livekitUrl: String,
) : BaseViewModel<CallState, CallEffect>(CallState()) {
    private val callId get() = args.callId
    private val conversationId get() = args.conversationId
    private val roomName get() = args.roomName
    private val callType get() = args.callType
    private val isOutgoing get() = args.isOutgoing
    private val isGroup get() = args.isGroup

    val mediaProjectionManager: MediaProjectionManager =
        application.getSystemService(MediaProjectionManager::class.java)

    private var room: Room? = null
    private var durationJob: Job? = null
    private var missedCallJob: Job? = null

    private var currentUserId: String? = null
    private var callMessageSent = false
    private val callMessageMutex = Mutex()

    init {
        AppLogger.d(TAG, "init callId=$callId isOutgoing=$isOutgoing isGroup=$isGroup")
        viewModelScope.launch { joinCall() }
        if (!isGroup) {
            viewModelScope.launch { observeRemoteStatus() }
            viewModelScope.launch { observeHangupSignal() }
        }
    }

    private suspend fun observeHangupSignal() {
        AppLogger.d(TAG, "observeHangupSignal: start listening callId=$callId")
        callRepository.observeHangupSignal(callId).collect {
            AppLogger.d(TAG, "observeHangupSignal: RECEIVED hangup signal, phase=${state.value.phase}")
            endCallLocally("ended")
        }
    }

    private suspend fun observeRemoteStatus() {
        AppLogger.d(TAG, "observeRemoteStatus: start listening callId=$callId")
        callRepository.observeCallStatus(callId).collect { status ->
            AppLogger.d(TAG, "observeRemoteStatus: status=$status phase=${state.value.phase}")
            if (status == CallStatus.REJECTED || status == CallStatus.ENDED) {
                endCallLocally(status.wireValue)
            }
        }
    }

    /**
     * Transitions to [CallPhase.ENDED] (a no-op if already there), disconnects the LiveKit room,
     * and sends the call-summary chat message with [summaryStatus]. Consolidates three call sites
     * (hangup signal, remote status going ENDED/REJECTED, a participant disconnecting) that used
     * to each hand-roll this sequence — one of them (ParticipantDisconnected) had drifted out of
     * sync and never called `room?.disconnect()`.
     */
    private fun endCallLocally(summaryStatus: String) {
        if (state.value.phase == CallPhase.ENDED) return
        durationJob?.cancel()
        updateState { it.copy(phase = CallPhase.ENDED) }
        catchResult { room?.disconnect() }
        viewModelScope.launch(Dispatchers.IO) {
            sendCallSummaryMessage(summaryStatus)
        }
    }

    private suspend fun joinCall() {
        catchResult {
            AppLogger.d(TAG, "joinCall: fetching user")
            val user = withTimeout(5_000L) {
                getCurrentUserUseCase().filterNotNull().first()
            }
            currentUserId = user.id
            AppLogger.d(TAG, "joinCall: userId=${user.id} roomName=$roomName livekitUrl=$livekitUrl")
            val token = callRepository.fetchLivekitToken(roomName, user.id)

            val livekitRoom = LiveKit.create(application)
            room = livekitRoom
            updateState { it.copy(room = livekitRoom) }

            viewModelScope.launch {
                livekitRoom.events.events.collect { event -> handleRoomEvent(event) }
            }
            viewModelScope.launch {
                livekitRoom.localParticipant.events.events.collect { event ->
                    if (event is ParticipantEvent.LocalTrackPublished) {
                        val track = event.publication.track
                        if (track is LocalVideoTrack) {
                            updateState { it.copy(localVideoTrack = track) }
                        }
                    }
                }
            }

            AppLogger.d(TAG, "joinCall: connecting to LiveKit room")
            livekitRoom.connect(livekitUrl, token)
            AppLogger.d(TAG, "joinCall: connected, remoteParticipants=${livekitRoom.remoteParticipants.size}")

            // For incoming calls: accept immediately after connecting to LiveKit so the DB
            // status is updated even if subsequent track publishing fails. This ensures the
            // caller knows the receiver has joined the room before audio is enabled.
            if (!isOutgoing) {
                AppLogger.d(TAG, "joinCall: accepting call callId=$callId")
                catchResult { callRepository.acceptCall(callId) }
                    .onFailure { e -> AppLogger.e(TAG, "joinCall: acceptCall FAILED (non-fatal)", e) }
            }

            val newPhase = if (isOutgoing) CallPhase.RINGING else CallPhase.ACTIVE
            updateState { it.copy(phase = newPhase) }
            if (!isOutgoing) startDurationTimer()

            if (isOutgoing) {
                missedCallJob = viewModelScope.launch {
                    delay(MISSED_CALL_TIMEOUT_MS)
                    if (state.value.phase == CallPhase.RINGING) {
                        AppLogger.d(TAG, "joinCall: missed call timeout reached")
                        hangUp()
                    }
                }
            }

            // Enable microphone — wrapped separately so that audio hardware errors (e.g.
            // AudioRecord already in use, RECORD_AUDIO denied after navigation) do NOT kill
            // the LiveKit connection. The call can still proceed (visually connected) and the
            // user can toggle the mic from the UI to retry.
            // NOTE: LIVEKIT_URL in local.properties must be reachable from the emulator.
            // wss://chatapp-8ff7ks6x.livekit.cloud is a public cloud URL and is accessible
            // from Android emulators through the host machine's network.
            catchResult { livekitRoom.localParticipant.setMicrophoneEnabled(true) }
                .onSuccess { AppLogger.d(TAG, "joinCall: microphone enabled") }
                .onFailure { e -> AppLogger.e(TAG, "joinCall: setMicrophoneEnabled FAILED (non-fatal) — call connected but no local audio", e) }

            if (callType == "video") {
                catchResult { livekitRoom.localParticipant.setCameraEnabled(true) }
                    .onSuccess { AppLogger.d(TAG, "joinCall: camera enabled") }
                    .onFailure { e -> AppLogger.e(TAG, "joinCall: setCameraEnabled FAILED (non-fatal)", e) }
            }

            if (livekitRoom.remoteParticipants.isNotEmpty() && state.value.phase != CallPhase.ACTIVE) {
                AppLogger.d(TAG, "joinCall: remote participants already present, switching to ACTIVE")
                missedCallJob?.cancel()
                updateState { it.copy(phase = CallPhase.ACTIVE) }
                startDurationTimer()
                // Also append to remoteVideoTracks (not just the singular remoteVideoTrack) —
                // the group grid renders from remoteVideoTracks, so a participant already in the
                // room at connect time was previously invisible in the grid.
                val existingTracks = livekitRoom.remoteParticipants.values.mapNotNull { p ->
                    p.videoTrackPublications
                        .mapNotNull { (_, track) -> track as? VideoTrack }
                        .firstOrNull()
                }
                if (existingTracks.isNotEmpty()) {
                    updateState { s ->
                        s.copy(
                            remoteVideoTrack = existingTracks.first(),
                            remoteVideoTracks = (s.remoteVideoTracks + existingTracks).distinct(),
                        )
                    }
                }
            }

        }.onFailure { e ->
            AppLogger.e(TAG, "joinCall: FAILED", e)
            updateState { it.copy(phase = CallPhase.ERROR, error = e.message ?: "Error al conectar") }
        }
    }

    private fun handleRoomEvent(event: RoomEvent) {
        AppLogger.d(TAG, "handleRoomEvent: ${event::class.simpleName}")
        when (event) {
            is RoomEvent.ParticipantConnected -> {
                AppLogger.d(TAG, "ParticipantConnected: identity=${event.participant.identity}")
                missedCallJob?.cancel()
                missedCallJob = null
                if (state.value.phase == CallPhase.RINGING || state.value.phase == CallPhase.CONNECTING) {
                    updateState { it.copy(phase = CallPhase.ACTIVE) }
                    startDurationTimer()
                }
            }
            is RoomEvent.ParticipantDisconnected -> {
                AppLogger.d(TAG, "ParticipantDisconnected: identity=${event.participant.identity} phase=${state.value.phase}")
                val phase = state.value.phase
                if (!isGroup && phase != CallPhase.ENDED) {
                    endCallLocally(if (phase == CallPhase.ACTIVE) "ended" else "missed")
                }
            }
            is RoomEvent.TrackSubscribed -> {
                val subscribedTrack = event.track
                if (subscribedTrack is VideoTrack) {
                    updateState { callState ->
                        callState.copy(
                            remoteVideoTrack = subscribedTrack,
                            remoteVideoTracks = (callState.remoteVideoTracks + subscribedTrack).distinct(),
                        )
                    }
                }
            }
            is RoomEvent.TrackMuted -> {
                if (event.publication.track is VideoTrack && event.participant !is io.livekit.android.room.participant.LocalParticipant) {
                    AppLogger.d(TAG, "TrackMuted: remote video muted")
                    updateState { it.copy(isRemoteVideoMuted = true) }
                }
            }
            is RoomEvent.TrackUnmuted -> {
                if (event.publication.track is VideoTrack && event.participant !is io.livekit.android.room.participant.LocalParticipant) {
                    AppLogger.d(TAG, "TrackUnmuted: remote video unmuted")
                    updateState { it.copy(isRemoteVideoMuted = false) }
                }
            }
            is RoomEvent.TrackUnsubscribed -> {
                val removedTrack = event.track as? VideoTrack
                if (removedTrack != null) {
                    updateState { callState ->
                        val updated = callState.remoteVideoTracks.filter { it !== removedTrack }
                        callState.copy(
                            remoteVideoTrack = updated.lastOrNull(),
                            remoteVideoTracks = updated,
                        )
                    }
                }
            }
            is RoomEvent.Disconnected -> {
                durationJob?.cancel()
                AppLogger.e(TAG, "RoomEvent.Disconnected: reason=${event.reason} error=${event.error?.message}")
                if (event.reason == DisconnectReason.CLIENT_INITIATED) {
                    updateState { it.copy(phase = CallPhase.ENDED) }
                } else {
                    updateState { it.copy(
                        phase = CallPhase.ERROR,
                        error = "${event.reason.name}: ${event.error?.message ?: "desconexión inesperada"}"
                    ) }
                }
            }
            is RoomEvent.FailedToConnect -> {
                AppLogger.e(TAG, "RoomEvent.FailedToConnect: ${event.error.message}")
                updateState { it.copy(phase = CallPhase.ERROR, error = "Error al conectar: ${event.error.message}") }
            }
            else -> {}
        }
    }

    private suspend fun sendCallSummaryMessage(status: String) = callMessageMutex.withLock {
        AppLogger.d(TAG, "sendCallSummaryMessage: status=$status isOutgoing=$isOutgoing callMessageSent=$callMessageSent")
        if (!isOutgoing || callMessageSent) return@withLock
        if (isGroup && status != "missed") return@withLock
        callMessageSent = true
        val userId = currentUserId ?: return@withLock
        val duration = if (status == "ended") state.value.durationSeconds else null
        sendMessageUseCase(
            conversationId = conversationId,
            senderId = userId,
            content = "",
            callType = callType,
            callStatus = status,
            callDuration = duration,
        ).onFailure { e -> AppLogger.e(TAG, "sendCallSummaryMessage: FAILED", e) }
         .onSuccess { AppLogger.d(TAG, "sendCallSummaryMessage: OK status=$status") }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                updateState { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    fun toggleMic() {
        val muted = state.value.isMicMuted
        viewModelScope.launch {
            catchResult { room?.localParticipant?.setMicrophoneEnabled(muted) }
            updateState { it.copy(isMicMuted = !muted) }
        }
    }

    fun toggleCamera() {
        val off = state.value.isCameraOff
        viewModelScope.launch {
            catchResult { room?.localParticipant?.setCameraEnabled(off) }
            if (!off) {
                updateState { it.copy(isCameraOff = true, localVideoTrack = null) }
            } else {
                val cameraTrack = room?.localParticipant
                    ?.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
                updateState { it.copy(isCameraOff = false, localVideoTrack = cameraTrack) }
            }
        }
    }

    fun switchCamera() {
        val front = state.value.isFrontCamera
        val newPosition = if (front) CameraPosition.BACK else CameraPosition.FRONT
        viewModelScope.launch {
            val cameraTrack = room?.localParticipant
                ?.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
            catchResult { cameraTrack?.switchCamera(position = newPosition) }
            updateState { it.copy(isFrontCamera = !front, localVideoTrack = cameraTrack) }
        }
    }

    fun toggleBackgroundBlur() {
        updateState { it.copy(isBackgroundBlurred = !it.isBackgroundBlurred) }
    }

    fun onIntent(intent: CallIntent) {
        when (intent) {
            is CallIntent.ToggleScreenShare -> {
                if (state.value.isScreenSharing) {
                    stopScreenShare()
                } else {
                    sendEffect(CallEffect.RequestScreenShare)
                }
            }
        }
    }

    fun startScreenShare(mediaProjectionData: Intent) {
        viewModelScope.launch {
            AppLogger.d(TAG, "startScreenShare: enabling screen share")
            catchResult {
                room?.localParticipant?.setScreenShareEnabled(true, ScreenCaptureParams(mediaProjectionData))
            }.onSuccess {
                AppLogger.d(TAG, "startScreenShare: OK")
                updateState { it.copy(isScreenSharing = true) }
            }.onFailure { e ->
                AppLogger.e(TAG, "startScreenShare: FAILED", e)
            }
        }
    }

    fun stopScreenShare() {
        viewModelScope.launch {
            AppLogger.d(TAG, "stopScreenShare: disabling screen share")
            catchResult {
                room?.localParticipant?.setScreenShareEnabled(false)
            }.onSuccess {
                AppLogger.d(TAG, "stopScreenShare: OK")
                updateState { it.copy(isScreenSharing = false) }
            }.onFailure { e ->
                AppLogger.e(TAG, "stopScreenShare: FAILED", e)
            }
        }
    }

    fun toggleInCallChat() {
        updateState { it.copy(showInCallChat = !it.showInCallChat) }
    }

    fun sendInCallMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val userId = currentUserId ?: return
        updateState {
            it.copy(inCallMessages = it.inCallMessages + InCallMessage(sender = userId, text = trimmed))
        }
        viewModelScope.launch(Dispatchers.IO) {
            sendMessageUseCase(
                conversationId = conversationId,
                senderId = userId,
                content = trimmed,
            ).onFailure { e -> AppLogger.e(TAG, "sendInCallMessage: FAILED", e) }
             .onSuccess { AppLogger.d(TAG, "sendInCallMessage: OK") }
        }
    }

    fun hangUp() {
        val phaseBefore = state.value.phase
        AppLogger.d(TAG, "hangUp: called phaseBefore=$phaseBefore callId=$callId")
        if (phaseBefore == CallPhase.ENDED) {
            AppLogger.d(TAG, "hangUp: already ENDED, ignoring")
            return
        }
        missedCallJob?.cancel()
        durationJob?.cancel()
        updateState { it.copy(phase = CallPhase.ENDED) }
        viewModelScope.launch {
            AppLogger.d(TAG, "hangUp: disconnecting room")
            catchResult { room?.disconnect() }
            AppLogger.d(TAG, "hangUp: room disconnect done")
        }
        viewModelScope.launch(Dispatchers.IO) {
            AppLogger.d(TAG, "hangUp: sending hangup signal callId=$callId")
            catchResult { callRepository.sendHangupSignal(callId) }
                .onSuccess { AppLogger.d(TAG, "hangUp: sendHangupSignal OK") }
                .onFailure { e -> AppLogger.e(TAG, "hangUp: sendHangupSignal FAILED", e) }
            AppLogger.d(TAG, "hangUp: calling endCall callId=$callId")
            catchResult { callRepository.endCall(callId) }
                .onSuccess { AppLogger.d(TAG, "hangUp: endCall OK") }
                .onFailure { e -> AppLogger.e(TAG, "hangUp: endCall FAILED", e) }
            sendCallSummaryMessage(if (phaseBefore == CallPhase.ACTIVE) "ended" else "missed")
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.d(TAG, "onCleared: callId=$callId")
        missedCallJob?.cancel()
        durationJob?.cancel()
        // Screen share is stopped automatically when the room disconnects below
        catchResult { room?.disconnect() }
        room = null
    }

    companion object {
        private const val TAG = "CallViewModel"
    }
}
