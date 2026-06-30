package com.ajrpachon.chatapp.ui.call

import android.app.Activity
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.CallRoute
import com.github.skydoves.navgraph.annotations.NavDestination
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
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
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (callType == "video") add(Manifest.permission.CAMERA)
        }.toTypedArray()
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
            modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text("Solicitando permisos...", color = Color.White.copy(alpha = 0.7f))
            }
        }
        return
    }

    CallScreenContent(callId, conversationId, roomName, callType, otherUserName, isOutgoing, isGroup, onCallEnded)
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
        parameters = { parametersOf(callId, conversationId, roomName, callType, isOutgoing, isGroup) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val currentRoom by vm.roomFlow.collectAsStateWithLifecycle()

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
        vm.effects.collect { effect ->
            when (effect) {
                is CallEffect.RequestScreenShare -> {
                    val mgr = context.getSystemService(MediaProjectionManager::class.java)
                    screenShareLauncher.launch(mgr.createScreenCaptureIntent())
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
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
                    BlurredVideoView(
                        track = localVideo,
                        room = currentRoom,
                        blurred = state.isBackgroundBlurred,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Screen share active banner
        if (state.isScreenSharing) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(Color(0xCCFF5722), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.ScreenShare, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Compartiendo pantalla", color = Color.White, style = MaterialTheme.typography.labelMedium)
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
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
            }

            when (state.phase) {
                CallPhase.CONNECTING -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Conectando...", color = Color.White.copy(alpha = 0.7f))
                }
                CallPhase.RINGING -> Text(
                    "Llamando...",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                CallPhase.ACTIVE -> Text(
                    formatDuration(state.durationSeconds),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                CallPhase.ENDED -> Text(
                    "Llamada terminada",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                CallPhase.ERROR -> Text(
                    state.error ?: "Error en la llamada",
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
                onClick = { vm.toggleMic() },
                containerColor = if (state.isMicMuted) Color.White else Color.White.copy(alpha = 0.2f),
                iconTint = if (state.isMicMuted) Color.Black else Color.White,
            ) {
                Icon(
                    imageVector = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Micrófono",
                )
            }

            // Camera toggle (video calls only)
            if (callType == "video") {
                CallControlButton(
                    onClick = { vm.toggleCamera() },
                    containerColor = if (state.isCameraOff) Color.White else Color.White.copy(alpha = 0.2f),
                    iconTint = if (state.isCameraOff) Color.Black else Color.White,
                ) {
                    Icon(
                        imageVector = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Cámara",
                    )
                }
                // Switch front/back camera (only when camera is active)
                if (!state.isCameraOff) {
                    CallControlButton(
                        onClick = { vm.switchCamera() },
                        containerColor = Color.White.copy(alpha = 0.2f),
                        iconTint = Color.White,
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Voltear cámara")
                    }
                    // Background blur toggle (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CallControlButton(
                            onClick = { vm.toggleBackgroundBlur() },
                            containerColor = if (state.isBackgroundBlurred) Color.White else Color.White.copy(alpha = 0.2f),
                            iconTint = if (state.isBackgroundBlurred) Color.Black else Color.White,
                        ) {
                            Icon(
                                imageVector = if (state.isBackgroundBlurred) Icons.Default.BlurOn else Icons.Default.BlurOff,
                                contentDescription = "Fondo borroso",
                            )
                        }
                    }
                }
            }

            // Screen share toggle
            CallControlButton(
                onClick = { vm.processIntent(CallIntent.ToggleScreenShare) },
                containerColor = if (state.isScreenSharing) Color(0xFFFF5722) else Color.White.copy(alpha = 0.2f),
                iconTint = Color.White,
            ) {
                Icon(
                    imageVector = if (state.isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                    contentDescription = "Compartir pantalla",
                )
            }

            // Hang up
            CallControlButton(
                onClick = { vm.hangUp() },
                containerColor = MaterialTheme.colorScheme.error,
                iconTint = Color.White,
                size = 64.dp,
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Colgar")
            }
        }

        // Chat FAB (bottom-start)
        IconButton(
            onClick = { vm.toggleInCallChat() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 64.dp)
                .size(52.dp)
                .background(
                    color = if (state.showInCallChat) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                ),
        ) {
            Icon(Icons.Default.Chat, contentDescription = "Chat en llamada", tint = Color.White)
        }

        // In-call chat panel
        AnimatedVisibility(
            visible = state.showInCallChat,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            InCallChatPanel(
                messages = state.inCallMessages,
                onSend = { vm.sendInCallMessage(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 140.dp),
            )
        }
    }
}

@Composable
private fun CallControlButton(
    onClick: () -> Unit,
    containerColor: Color,
    iconTint: Color,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    content: @Composable () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = iconTint,
        ),
    ) { content() }
}

// ── Video rendering ───────────────────────────────────────────────────────────

@Composable
fun VideoView(track: VideoTrack, room: Room? = null, modifier: Modifier = Modifier) {
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

@Composable
private fun BlurredVideoView(
    track: VideoTrack,
    room: Room?,
    blurred: Boolean,
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
            update = { view ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    view.setRenderEffect(
                        if (blurred) android.graphics.RenderEffect.createBlurEffect(
                            25f, 25f, android.graphics.Shader.TileMode.CLAMP
                        ) else null
                    )
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

// ── In-call chat panel ────────────────────────────────────────────────────────

@Composable
private fun InCallChatPanel(
    messages: List<InCallMessage>,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp)
            .imePadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(messages) { msg ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = msg.sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensaje...", color = Color.White.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) { onSend(text.trim()); text = "" }
                }),
                singleLine = true,
            )
            IconButton(
                onClick = {
                    if (text.isNotBlank()) { onSend(text.trim()); text = "" }
                },
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDuration(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
