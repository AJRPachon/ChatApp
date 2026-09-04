package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.StickerValidation
import com.ajrpachon.chatapp.ui.common.formatCallDuration
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Message-type-specific bubbles ────────────────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — part of the message-bubble
// rendering tree, rendered from MessageBubble.kt via ChatBubbleSlot (defined there).

@Composable
internal fun CallMessageBubble(message: MessageBO) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val isVideo = message.callType == "video"
    val status = message.callStatus ?: "ended"

    val iconTint = when {
        status == "missed" && !message.isFromMe -> MaterialTheme.colorScheme.error
        status == "ended" -> com.ajrpachon.chatapp.ui.theme.CallAcceptedGreen
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val statusText = when {
        status == "ended" -> stringResource(R.string.chat_call_ended_duration, formatCallDuration(message.callDuration ?: 0))
        status == "missed" && !message.isFromMe -> stringResource(R.string.chat_call_missed)
        status == "rejected" && !message.isFromMe -> stringResource(R.string.chat_call_rejected)
        else -> stringResource(R.string.chat_call_no_answer)
    }
    val statusColor = when {
        status == "missed" && !message.isFromMe -> MaterialTheme.colorScheme.error
        status == "ended" -> com.ajrpachon.chatapp.ui.theme.CallAcceptedGreen
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    }

    ChatBubbleSlot(isFromMe = message.isFromMe) { maxBubbleWidth ->
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = maxBubbleWidth),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.widthIn(min = 100.dp)) {
                    Text(
                        text = if (isVideo) stringResource(R.string.chat_video_call) else stringResource(R.string.chat_voice_call),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
    }
}

@Composable
internal fun FileBubble(
    message: MessageBO,
    onOpenPdf: (url: String, filename: String) -> Unit = { _, _ -> },
    onLongPress: () -> Unit = {},
) {
    val isPdf = message.fileUrl?.endsWith(".pdf", ignoreCase = true) == true
    if (isPdf) {
        PdfFileCard(message = message, onOpenPdf = onOpenPdf)
    } else {
        GenericFileBubble(message = message, onLongPress = onLongPress)
    }
}

@Composable
private fun PdfFileCard(
    message: MessageBO,
    onOpenPdf: (url: String, filename: String) -> Unit,
) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val bubbleColor = if (message.isFromMe)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val documentPdfDefault = stringResource(R.string.chat_document_pdf_default)

    ChatBubbleSlot(isFromMe = message.isFromMe, modifier = Modifier.padding(vertical = 2.dp)) { maxBubbleWidth ->
        Surface(
            shape = MaterialTheme.shapes.small,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = minOf(280.dp, maxBubbleWidth)),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.fileName ?: documentPdfDefault,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (message.fileSize != null) {
                            Text(
                                text = formatFileSize(message.fileSize),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Bottom),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val url = message.fileUrl ?: return@clickable
                            val filename = message.fileName ?: documentPdfDefault
                            onOpenPdf(url, filename)
                        },
                ) {
                    Text(
                        text = stringResource(R.string.chat_view_pdf),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GenericFileBubble(message: MessageBO, onLongPress: () -> Unit = {}) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val bubbleColor = if (message.isFromMe)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val context = LocalContext.current

    ChatBubbleSlot(isFromMe = message.isFromMe, modifier = Modifier.padding(vertical = 2.dp)) { maxBubbleWidth ->
        Surface(
            shape = MaterialTheme.shapes.small,
            color = bubbleColor,
            // combinedClickable (not a plain clickable) so a long-press here can still reach
            // onLongPress instead of always firing onClick regardless of hold duration — same fix
            // as MessageBubble's bare-image AsyncImage, needed for the same reason: without it,
            // there's no way to long-press-to-select a file message for bulk delete.
            modifier = Modifier.widthIn(max = minOf(280.dp, maxBubbleWidth))
                .combinedClickable(
                    onClick = {
                        message.fileUrl?.let { url ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse(url)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onLongClick = onLongPress,
                ),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.fileName ?: stringResource(R.string.chat_file_default),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (message.fileSize != null) {
                        Text(
                            text = formatFileSize(message.fileSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
    }
}

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun VideoBubble(message: MessageBO, onLongPress: () -> Unit = {}) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val context = LocalContext.current

    ChatBubbleSlot(isFromMe = message.isFromMe, modifier = Modifier.padding(vertical = 2.dp)) { maxBubbleWidth ->
        Column(
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                // combinedClickable, not a plain clickable — see GenericFileBubble's onLongPress
                // doc above for why (same bare-`clickable`-swallows-long-press issue).
                modifier = Modifier.widthIn(max = minOf(240.dp, maxBubbleWidth)).combinedClickable(
                    onClick = {
                        val uri = android.net.Uri.parse(message.videoUrl)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    onLongClick = onLongPress,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = message.videoUrl,
                        contentDescription = stringResource(R.string.chat_video_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.chat_play),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
internal fun StickerBubble(message: MessageBO) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    ChatBubbleSlot(isFromMe = message.isFromMe, modifier = Modifier.padding(vertical = 2.dp)) {
        Column(horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start) {
            val sticker = StickerValidation.sanitize(message.stickerUrl)
            if (sticker != null) {
                Text(text = sticker, fontSize = 64.sp)
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
internal fun DeletedMessageBubble(message: MessageBO) {
    ChatBubbleSlot(isFromMe = message.isFromMe, modifier = Modifier.padding(vertical = 2.dp)) { maxBubbleWidth ->
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.widthIn(max = maxBubbleWidth),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    stringResource(R.string.chat_message_deleted),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
