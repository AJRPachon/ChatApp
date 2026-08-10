package com.ajrpachon.chatapp.ui.status

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.ui.common.ChatConstants
import com.ajrpachon.chatapp.ui.common.formatStatusAge
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel

// ── Status bar (embedded in ConversationListScreen) ────────────────────────

@Composable
fun StatusBar(
    onViewStatus: (StatusBO) -> Unit,
    modifier: Modifier = Modifier,
    vm: StatusViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // A single picker covering both photos and videos; the mime type of what
    // the user actually picked decides which intent to dispatch.
    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it).orEmpty()
            if (mimeType.startsWith("video/")) {
                vm.onIntent(StatusIntent.PostVideoStatus(it))
            } else {
                vm.onIntent(StatusIntent.PostImageStatus(it))
            }
        }
    }

    // Own statuses are represented by the "My status" slot below, not as a
    // separate contact-like avatar mixed in with everyone else's.
    val myStatuses = state.statuses.filter { it.isFromMe }
    val contactStatuses = state.statuses.filterNot { it.isFromMe }

    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // "My status" slot: the add prompt while empty, or a single avatar
            // (with a "+" badge to add another) once an own status exists —
            // mirrors Instagram/WhatsApp instead of showing two circles.
            item {
                val myStatus = myStatuses.firstOrNull()
                if (myStatus == null) {
                    AddStatusButton(
                        onAddText = { vm.onIntent(StatusIntent.OpenCompose) },
                        onAddMedia = {
                            mediaLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        },
                    )
                } else {
                    MyStatusAvatar(
                        status = myStatus,
                        onView = { onViewStatus(myStatus) },
                        onAddText = { vm.onIntent(StatusIntent.OpenCompose) },
                        onAddMedia = {
                            mediaLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        },
                    )
                }
            }
            items(contactStatuses, key = { it.id }) { status ->
                StatusAvatar(status = status, onClick = { onViewStatus(status) })
            }
        }
    }

    if (state.showComposeDialog) {
        ComposeStatusDialog(
            text = state.composeText,
            onTextChange = { vm.onIntent(StatusIntent.TextChanged(it)) },
            onPost = { vm.onIntent(StatusIntent.PostTextStatus) },
            onDismiss = { vm.onIntent(StatusIntent.CloseCompose) },
        )
    }
}

@Composable
private fun AddStatusButton(
    onAddText: () -> Unit,
    onAddMedia: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAddText),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.status_add_text_cd),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onAddMedia),
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = stringResource(R.string.status_add_image_cd),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.status_my_status), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MyStatusAvatar(
    status: StatusBO,
    onView: () -> Unit,
    onAddText: () -> Unit,
    onAddMedia: () -> Unit,
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(56.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(interactionSource = null, indication = null, onClick = onView),
            ) {
                ChatAppAvatar(name = status.userName, url = status.userAvatarUrl, size = 52.dp)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showAddMenu = true },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.status_add_text_cd),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.status_add_text_cd)) },
                        onClick = { showAddMenu = false; onAddText() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.status_add_image_cd)) },
                        onClick = { showAddMenu = false; onAddMedia() },
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.status_my_status),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusAvatar(status: StatusBO, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .clickable(interactionSource = null, indication = null, onClick = onClick),
        ) {
            ChatAppAvatar(
                name = status.userName,
                url = status.userAvatarUrl,
                size = 52.dp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = status.userName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ComposeStatusDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.status_new_status_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.status_compose_placeholder)) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onPost, enabled = text.isNotBlank()) { Text(stringResource(R.string.status_publish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.status_cancel)) }
        },
    )
}

// ── Full-screen story viewer ───────────────────────────────────────────────

@Composable
fun StatusViewerScreen(
    statuses: List<StatusBO>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val current = statuses.getOrNull(currentIndex) ?: run { onClose(); return }
    val isVideo = current.videoUrl != null

    // Text/image stories advance on a fixed timer; video stories advance when
    // playback finishes, with the progress bar following the player position
    // (set via videoProgress below) instead of a fixed-duration animation.
    val timedProgress = remember(currentIndex) { Animatable(0f) }
    var videoProgress by remember(currentIndex) { mutableFloatStateOf(0f) }

    LaunchedEffect(currentIndex, isVideo) {
        if (!isVideo) {
            timedProgress.snapTo(0f)
            timedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(ChatConstants.STORY_DURATION_MS.toInt(), easing = LinearEasing),
            )
            if (currentIndex < statuses.lastIndex) currentIndex++ else onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(current.backgroundColor)),
    ) {
        // Media fills the entire screen; the header floats on top of it.
        when {
            current.videoUrl != null -> StoryVideoPlayer(
                url = current.videoUrl,
                onProgress = { videoProgress = it },
                onCompleted = { if (currentIndex < statuses.lastIndex) currentIndex++ else onClose() },
                modifier = Modifier.fillMaxSize(),
            )
            current.imageUrl != null -> AsyncImage(
                model = current.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!current.text.isNullOrBlank()) {
            Text(
                text = current.text,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            )
        }

        // Header (avatar bubble + name + progress bar), on top of the media.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp),
            ) {
                ChatAppAvatar(
                    name = current.userName,
                    url = current.userAvatarUrl,
                    size = 36.dp,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.userName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatStatusAge(current.createdAt),
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                statuses.forEachIndexed { idx, _ ->
                    val segmentProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> if (isVideo) videoProgress else timedProgress.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        }

        // Tap zones to navigate — no ripple: a full-screen highlight on every
        // tap would be distracting for a story viewer (Instagram/WhatsApp
        // don't show one either).
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) {
                        if (currentIndex > 0) currentIndex-- else onClose()
                    },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) {
                        if (currentIndex < statuses.lastIndex) currentIndex++ else onClose()
                    },
            )
        }
    }
}

// Convenience: launch the viewer for a single user's stories, sourced from the
// same StatusViewModel that already observes active statuses — used by the
// StatusViewerRoute nav entry, which only has the tapped user's id to go on.
@Composable
fun StatusViewerScreen(
    userId: String,
    onClose: () -> Unit,
    vm: StatusViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.statuses, userId) {
        vm.onIntent(StatusIntent.FilterUserStatuses(state.statuses, userId))
    }

    StatusViewerScreen(
        statuses = state.userStatuses,
        initialIndex = 0,
        onClose = onClose,
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun StoryVideoPlayer(
    url: String,
    onProgress: (Float) -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onCompleted()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll playback position for the progress bar — ExoPlayer has no
    // continuous progress flow, so this is the standard workaround.
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            val duration = exoPlayer.duration
            if (duration > 0) onProgress((exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            delay(100)
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier,
    )
}
