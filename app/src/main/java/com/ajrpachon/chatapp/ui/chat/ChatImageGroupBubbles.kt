package com.ajrpachon.chatapp.ui.chat

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Image-batch bubbles ───────────────────────────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — part of the message-bubble
// rendering tree. Called directly from ChatScreen's LazyColumn (not from MessageBubble):
// PendingImageBatchBubble is the in-flight upload placeholder, ImageGroupBubble is the
// server-backed >2-photo group it hands off to once the batch finishes uploading — see
// PendingImageBatchBubble's own doc for why they mirror each other's layout exactly.

// Placeholder shown while a multi-image batch (>2 photos) uploads, mirroring ImageGroupBubble's
// layout exactly (same 2-cell grid + overlay) so there is no shape/size change when the real,
// server-backed group replaces it once the batch finishes — that's what removes the visible jump.
@Composable
internal fun PendingImageBatchBubble(
    uris: List<Uri>,
    progress: MediaUploadProgress?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AsyncImage(
                model = uris[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp),
            ) {
                AsyncImage(
                    model = uris[1],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "${progress?.completedCount ?: 0}/${progress?.totalCount ?: uris.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImageGroupBubble(
    messages: List<MessageBO>,
    onImageClick: (index: Int) -> Unit,
    onReply: () -> Unit,
) {
    val isFromMe = messages.first().isFromMe
    val urls = messages.mapNotNull { it.imageUrl }
    val timeText = remember(messages.last().createdAt) {
        val local = messages.last().createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset.value >= swipeThreshold) onReply()
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onDragCancel = {
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(0f, swipeThreshold * 1.3f))
                        }
                    },
                )
            },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha((swipeOffset.value / swipeThreshold).coerceIn(0f, 1f)),
        )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = swipeOffset.value },
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
            Row(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AsyncImage(
                    model = urls[0],
                    contentDescription = stringResource(R.string.chat_image_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .clickable { onImageClick(0) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .clickable { onImageClick(1) },
                ) {
                    AsyncImage(
                        model = urls[1],
                        contentDescription = stringResource(R.string.chat_image_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = stringResource(R.string.chat_photos_count, urls.size),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    } // Box
}
