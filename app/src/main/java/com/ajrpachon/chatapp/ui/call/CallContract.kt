package com.ajrpachon.chatapp.ui.call

import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.VideoTrack

enum class CallPhase { CONNECTING, RINGING, ACTIVE, ENDED, ERROR }

data class CallState(
    val phase: CallPhase = CallPhase.CONNECTING,
    val isMicMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val remoteVideoTrack: VideoTrack? = null,
    val remoteVideoTracks: List<VideoTrack> = emptyList(),
    val localVideoTrack: LocalVideoTrack? = null,
    val durationSeconds: Int = 0,
    val error: String? = null,
    val isRemoteVideoMuted: Boolean = false,
)
