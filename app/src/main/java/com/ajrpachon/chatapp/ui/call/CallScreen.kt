package com.ajrpachon.chatapp.ui.call

import android.app.Activity
import android.content.pm.PackageManager
import com.ajrpachon.chatapp.ui.common.CallPermissions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.CallRoute
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.common.formatCallDuration
import com.ajrpachon.chatapp.ui.theme.CallBackground
import com.ajrpachon.chatapp.ui.theme.CallScreenShareAccent
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import com.github.skydoves.navgraph.annotations.NavDestination
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ── Active call screen ────────────────────────────────────────────────────────

@NavDestination(route = CallRoute::class)
@Composable
fun CallScreen(
    callId: String,
    conversationId: String,
    roomName: String,
    callType: String,
    otherUserName: String,
    isOutgoing: Boolean,
    isGroup: Boolean = false,
    onCallEnded: () -> Unit,
) {
    val context = LocalContext.current
    val required = remember(callType) {
        CallPermissions.forCallType(callType).toTypedArray()
    }
    var granted by remember {
        mutableStateOf(required.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result.values.all { it } }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(required)
    }

    if (!granted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CallBackground),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.call_requesting_permissions),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    CallScreenContent(
        callId,
        conversationId,
        roomName,
        callType,
        otherUserName,
        isOutgoing,
        isGroup,
        onCallEnded
    )
}

@Composable
private fun CallScreenContent(
    callId: String,
    conversationId: String,
    roomName: String,
    callType: String,
    otherUserName: String,
    isOutgoing: Boolean,
    isGroup: Boolean,
    onCallEnded: () -> Unit,
) {
    val context = LocalContext.current
    val vm: CallViewModel = koinViewModel(
        key = callId,
        parameters = {
            parametersOf(
                CallArgs(
                    callId,
                    conversationId,
                    roomName,
                    callType,
                    isOutgoing,
                    isGroup
                )
            )
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    // Screen share MediaProjection launcher
    val screenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { vm.startScreenShare(it) }
        }
    }

    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.ENDED || state.phase == CallPhase.ERROR) {
            onCallEnded()
        }
    }

    // Collect CallEffects (e.g. RequestScreenShare)
    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is CallEffect.RequestScreenShare -> {
                    screenShareLauncher.launch(vm.mediaProjectionManager.createScreenCaptureIntent())
                }
            }
        }
    }

    CallScreenBody(
        state = state,
        callType = callType,
        otherUserName = otherUserName,
        isGroup = isGroup,
        onToggleMic = { vm.toggleMic() },
        onToggleCamera = { vm.toggleCamera() },
        onSwitchCamera = { vm.switchCamera() },
        onToggleScreenShare = { vm.onIntent(CallIntent.ToggleScreenShare) },
        onHangUp = { vm.hangUp() },
    )
}

// Pure UI over [CallState] — no Koin/LiveKit wiring of its own, so it's directly usable from
// @Preview below with a hand-built state (real video tracks still won't render without a live
// LiveKit Room, but every other visual: phase text, controls, colors, layout — is exactly what
// ships, so editing this function updates those previews live.
@Composable
private fun CallScreenBody(
    state: CallState,
    callType: String,
    otherUserName: String,
    isGroup: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onHangUp: () -> Unit,
) {
    val currentRoom = state.room

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBackground),
    ) {
        // Remote video — grid for groups, fullscreen for 1:1
        if (callType == "video") {
            val tracks = state.remoteVideoTracks
            when {
                isGroup && tracks.size > 1 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false,
                    ) {
                        gridItems(tracks) { track ->
                            VideoView(
                                track = track,
                                room = currentRoom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                            )
                        }
                    }
                }

                tracks.isNotEmpty() && !state.isRemoteVideoMuted -> VideoView(
                    track = tracks.first(),
                    room = currentRoom,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Local video (draggable PiP)
        val localVideo = state.localVideoTrack
        if (callType == "video" && localVideo != null && !state.isCameraOff) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val pipW = with(density) { 100.dp.toPx() }
                val pipH = with(density) { 140.dp.toPx() }
                val containerW = with(density) { maxWidth.toPx() }
                val containerH = with(density) { maxHeight.toPx() }
                val margin = with(density) { 16.dp.toPx() }
                val topMargin = with(density) { 60.dp.toPx() }

                var offsetX by remember { mutableStateOf(containerW - pipW - margin) }
                var offsetY by remember { mutableStateOf(topMargin) }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .size(100.dp, 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, containerW - pipW)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, containerH - pipH)
                            }
                        },
                ) {
                    VideoView(
                        track = localVideo,
                        room = currentRoom,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Switch front/back camera (video calls, only when camera is active)
        if (callType == "video" && !state.isCameraOff) {
            CallControlButton(
                onClick = onSwitchCamera,
                containerColor = Color.White.copy(alpha = 0.2f),
                iconTint = Color.White,
                size = 44.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp),
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.call_flip_camera_content_description)
                )
            }
        }

        // Screen share active banner
        if (state.isScreenSharing) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(CallScreenShareAccent.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.ScreenShare,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.call_screen_sharing_active),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Center content (name + status)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (callType != "video" || state.remoteVideoTrack == null) {
                CallPartyHeader(otherUserName)
                Spacer(Modifier.height(8.dp))
            }

            when (state.phase) {
                CallPhase.CONNECTING -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.call_connecting),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                CallPhase.RINGING -> Text(
                    stringResource(R.string.call_ringing),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                )

                CallPhase.ACTIVE -> Text(
                    formatCallDuration(state.durationSeconds),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                )

                CallPhase.ENDED -> Text(
                    stringResource(R.string.call_ended),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                )

                CallPhase.ERROR -> Text(
                    state.error ?: stringResource(R.string.call_generic_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Controls (bottom)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mute mic
            CallControlButton(
                onClick = onToggleMic,
                containerColor = if (state.isMicMuted) Color.White else Color.White.copy(alpha = 0.2f),
                iconTint = if (state.isMicMuted) Color.Black else Color.White,
            ) {
                Icon(
                    imageVector = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = stringResource(R.string.call_mic_content_description),
                )
            }

            // Camera toggle (video calls only)
            if (callType == "video") {
                CallControlButton(
                    onClick = onToggleCamera,
                    containerColor = if (state.isCameraOff) Color.White else Color.White.copy(alpha = 0.2f),
                    iconTint = if (state.isCameraOff) Color.Black else Color.White,
                ) {
                    Icon(
                        imageVector = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = stringResource(R.string.call_camera_content_description),
                    )
                }
            }

            // Screen share toggle
            CallControlButton(
                onClick = onToggleScreenShare,
                containerColor = if (state.isScreenSharing) CallScreenShareAccent else Color.White.copy(
                    alpha = 0.2f
                ),
                iconTint = Color.White,
            ) {
                Icon(
                    imageVector = if (state.isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                    contentDescription = stringResource(R.string.call_share_screen_content_description),
                )
            }

            // Hang up
            CallControlButton(
                onClick = onHangUp,
                containerColor = MaterialTheme.colorScheme.error,
                iconTint = Color.White,
                size = 64.dp,
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = stringResource(R.string.call_hang_up_content_description)
                )
            }
        }
    }
}

// ── Video rendering ───────────────────────────────────────────────────────────

/**
 * Renders [track] via a LiveKit [TextureViewRenderer].
 */
@Composable
fun VideoView(
    track: VideoTrack,
    room: Room? = null,
    modifier: Modifier = Modifier,
) {
    key(track) {
        AndroidView<TextureViewRenderer>(
            factory = { ctx ->
                TextureViewRenderer(ctx).also { view ->
                    room?.initVideoRenderer(view)
                    track.addRenderer(view)
                }
            },
            onRelease = { view ->
                track.removeRenderer(view)
                view.release()
            },
            modifier = modifier,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────
// Live-editable: these render the same CallScreenBody the real call screen uses, just fed a
// hand-built CallState instead of a wired-up CallViewModel/LiveKit Room — so tweaking colors,
// spacing, control layout, etc. above shows up here immediately (Android Studio's Split or
// Preview pane, Live Edit) without needing a real call, permissions, or a device. The one thing
// that can't render here is actual remote/local video — VideoTrack needs a live LiveKit Room —
// so the video-call previews below stay audio-shaped (icon + name) even though callType="video".

@Preview(name = "Voice — active", showBackground = true)
@Composable
internal fun CallScreenBodyActiveVoicePreview() {
    ChatAppTheme {
        CallScreenBody(
            state = CallState(phase = CallPhase.ACTIVE, durationSeconds = 125),
            callType = "audio",
            otherUserName = "Antonio Ramírez",
            isGroup = false,
            onToggleMic = {}, onToggleCamera = {}, onSwitchCamera = {},
            onToggleScreenShare = {}, onHangUp = {},
        )
    }
}

@Preview(name = "Video — connecting", showBackground = true)
@Composable
internal fun CallScreenBodyConnectingVideoPreview() {
    ChatAppTheme {
        CallScreenBody(
            state = CallState(phase = CallPhase.CONNECTING),
            callType = "video",
            otherUserName = "Claude QA",
            isGroup = false,
            onToggleMic = {}, onToggleCamera = {}, onSwitchCamera = {},
            onToggleScreenShare = {}, onHangUp = {},
        )
    }
}

@Preview(name = "Voice — ringing (outgoing)", showBackground = true)
@Composable
internal fun CallScreenBodyRingingPreview() {
    ChatAppTheme {
        CallScreenBody(
            state = CallState(phase = CallPhase.RINGING),
            callType = "audio",
            otherUserName = "Antonio Ramírez",
            isGroup = false,
            onToggleMic = {}, onToggleCamera = {}, onSwitchCamera = {},
            onToggleScreenShare = {}, onHangUp = {},
        )
    }
}

@Preview(name = "Voice — muted", showBackground = true)
@Composable
internal fun CallScreenBodyMutedPreview() {
    ChatAppTheme {
        CallScreenBody(
            state = CallState(
                phase = CallPhase.ACTIVE,
                durationSeconds = 42,
                isMicMuted = true,
            ),
            callType = "audio",
            otherUserName = "Antonio Ramírez",
            isGroup = false,
            onToggleMic = {}, onToggleCamera = {}, onSwitchCamera = {},
            onToggleScreenShare = {}, onHangUp = {},
        )
    }
}

@Preview(name = "Video — camera off + screen sharing", showBackground = true)
@Composable
internal fun CallScreenBodyScreenSharingPreview() {
    ChatAppTheme {
        CallScreenBody(
            state = CallState(
                phase = CallPhase.ACTIVE,
                durationSeconds = 300,
                isCameraOff = true,
                isScreenSharing = true,
            ),
            callType = "video",
            otherUserName = "Antonio Ramírez",
            isGroup = false,
            onToggleMic = {}, onToggleCamera = {}, onSwitchCamera = {},
            onToggleScreenShare = {}, onHangUp = {},
        )
    }
}

@Preview(name = "Voice — call ended", showBackground = true)
@Composable
internal fun CallScreenBodyEndedPreview() {
    ChatAppTheme {
        CallScreenBody(
            state = CallState(phase = CallPhase.ENDED),
            callType = "audio",
            otherUserName = "Antonio Ramírez",
            isGroup = false,
            onToggleMic = {}, onToggleCamera = {}, onSwitchCamera = {},
            onToggleScreenShare = {}, onHangUp = {},
        )
    }
}

