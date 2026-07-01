package com.ajrpachon.chatapp.ui.call

import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.VideoTrack

enum class CallPhase { CONNECTING, RINGING, ACTIVE, ENDED, ERROR }

/**
 * Represents a single remote participant in a call.
 * Used for group call grid rendering. [videoTrack] is null when the participant's camera is off.
 */
data class ParticipantState(
    val identity: String,
    val displayName: String,
    val videoTrack: VideoTrack?,
    val isMuted: Boolean,
    val isSpeaking: Boolean,
)

data class InCallMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class CallState(
    val phase: CallPhase = CallPhase.CONNECTING,
    val isMicMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isFrontCamera: Boolean = true,
    val remoteVideoTrack: VideoTrack? = null,
    val remoteVideoTracks: List<VideoTrack> = emptyList(),
    /** Structured participant list for group call grid UI. */
    val participants: List<ParticipantState> = emptyList(),
    val localVideoTrack: LocalVideoTrack? = null,
    val durationSeconds: Int = 0,
    val error: String? = null,
    val isRemoteVideoMuted: Boolean = false,
    val isBackgroundBlurred: Boolean = false,
    val isScreenSharing: Boolean = false,
    val showInCallChat: Boolean = false,
    val inCallMessages: List<InCallMessage> = emptyList(),
    val selectedFilter: CameraFilter = CameraFilter.NONE,
    val showFilterSheet: Boolean = false,
    val isRecording: Boolean = false,
    val recordingFilePath: String? = null,
)

sealed class CallIntent {
    object ToggleScreenShare : CallIntent()
    object OpenFilterSheet : CallIntent()
    object DismissFilterSheet : CallIntent()
    data class SetCameraFilter(val filter: CameraFilter) : CallIntent()
    object ToggleRecording : CallIntent()
}

sealed class CallEffect {
    object RequestScreenShare : CallEffect()
    data class ShowRecordingSaved(val path: String) : CallEffect()
}
