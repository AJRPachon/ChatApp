package com.ajrpachon.chatapp.ui.chat

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.LocationMessageFormat
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.SendStatus
import com.ajrpachon.chatapp.ui.components.EmojiPickerBottomSheet
import com.ajrpachon.chatapp.utils.LinkPreviewData

// ── Shared bubble content ────────────────────────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — part of the message-bubble
// rendering tree, rendered from MessageBubble.kt.

/**
 * Everything a bubble shows below its media (or in place of it, for a text-only message):
 * the audio player, the message text (with its long-press menu) or shared-location card, the
 * link preview, and the trailing time/edited/self-destruct/status row. Shared between the
 * plain-text bubble layout and the has-media bubble layout in [MessageBubble] so this fairly
 * involved block — dropdown menu, emoji picker, link detection — only exists once.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MessageFooterContent(
    message: MessageBO,
    timeText: String,
    onEdit: (() -> Unit)?,
    onSelfDestruct: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onForward: (() -> Unit)?,
    onCopy: (String) -> Unit,
    onToggleReaction: (String) -> Unit,
    linkPreviews: Map<String, LinkPreviewData?>,
    onDetectedUrl: (String) -> Unit,
    translatedText: String? = null,
    isTranslating: Boolean = false,
    onTranslate: () -> Unit = {},
    onDismissTranslation: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
) {
    if (message.audioUrl != null && MediaUrlValidator.isValid(message.audioUrl)) {
        RemoteAudioPlayer(
            url = message.audioUrl,
            senderAvatarUrl = message.senderAvatarUrl,
            senderInitial = message.senderName.firstOrNull()?.uppercase() ?: "?",
            sentTime = timeText,
            isFromMe = message.isFromMe,
            sendStatus = message.sendStatus,
            isRead = message.isRead,
        )
    }
    if (message.content.isNotBlank()) {
        var showMsgMenu by remember { mutableStateOf(false) }
        var showEmojiPicker by remember { mutableStateOf(false) }
        val emojiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val locationUrl = remember(message.content) {
            LocationMessageFormat.parseMapsUrl(message.content)
        }
        if (locationUrl != null) {
            LocationMessageCard(mapsUrl = locationUrl, onLongPress = onToggleSelect)
        }
        Box(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { showMsgMenu = true },
            ),
        ) {
            if (locationUrl == null) {
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = showMsgMenu, onDismissRequest = { showMsgMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_react)) },
                    leadingIcon = { Text("😊") },
                    onClick = { showMsgMenu = false; showEmojiPicker = true },
                )
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { showMsgMenu = false; onEdit() },
                    )
                }
                if (onSelfDestruct != null) {
                    DropdownMenuItem(
                        text = {
                            Text(if (message.expiresAt != null) stringResource(R.string.chat_remove_self_destruct) else stringResource(R.string.chat_ephemeral_message))
                        },
                        leadingIcon = { Text(if (message.expiresAt != null) "♾️" else "⏱️") },
                        onClick = { showMsgMenu = false; onSelfDestruct() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_copy)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = { showMsgMenu = false; onCopy(message.content) },
                )
                if (translatedText == null && !isTranslating) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_translate)) },
                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                        onClick = { showMsgMenu = false; onTranslate() },
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { showMsgMenu = false; onDelete() },
                    )
                }
                if (onForward != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_forward)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null) },
                        onClick = { showMsgMenu = false; onForward() },
                    )
                }
            }
        }
        if (showEmojiPicker) {
            EmojiPickerBottomSheet(
                sheetState = emojiSheetState,
                onDismiss = { showEmojiPicker = false },
                onEmojiSelected = { emoji -> onToggleReaction(emoji) },
            )
        }
        if (isTranslating) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Text(
                    text = stringResource(R.string.chat_translating),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        } else if (translatedText != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = translatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismissTranslation) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.chat_hide_translation),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
    // Link preview — shown only for plain text messages (no image/audio/gif)
    val hasAttachment = message.imageUrl != null || message.audioUrl != null ||
        message.gifUrl != null
    if (!hasAttachment && message.content.isNotBlank()) {
        val detectedUrl = remember(message.content) {
            val matcher = Patterns.WEB_URL.matcher(message.content)
            if (matcher.find()) matcher.group() else null
        }
        if (detectedUrl != null) {
            LaunchedEffect(detectedUrl) { onDetectedUrl(detectedUrl) }
            val previewData = linkPreviews[detectedUrl]
            if (previewData != null) {
                Spacer(Modifier.height(6.dp))
                LinkPreviewCard(data = previewData)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        message.expiresAt?.let { exp ->
            val secsLeft = ((exp - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            Text(
                "⏱️ ${if (secsLeft < 60) "${secsLeft}s" else "${secsLeft / 60}m"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        if (message.isEdited) {
            Text(
                stringResource(R.string.chat_edited_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        // Audio messages show their send time (and status icon) inside the
        // audio row instead (bottom-right of the waveform), so skip it here.
        if (message.audioUrl == null) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 2.dp),
            )
            if (message.isFromMe) {
                SendStatusIcon(sendStatus = message.sendStatus, isRead = message.isRead)
            }
        }
    }
}

private fun openLocationInMaps(context: android.content.Context, mapsUrl: String, latLng: Pair<Double, Double>?) {
    val gmmIntent = latLng?.let { (lat, lng) ->
        android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng"),
        ).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    val opened = gmmIntent != null && runCatching { context.startActivity(gmmIntent) }.isSuccess
    if (!opened) {
        val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LocationMessageCard(mapsUrl: String, onLongPress: () -> Unit = {}) {
    val context = LocalContext.current
    val latLng = remember(mapsUrl) {
        runCatching {
            val query = android.net.Uri.parse(mapsUrl).getQueryParameter("q") ?: return@runCatching null
            val (lat, lng) = query.split(",").map { it.trim().toDouble() }
            lat to lng
        }.getOrNull()
    }
    val staticMapUrl = remember(latLng) {
        latLng?.let { (lat, lng) ->
            "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lng&zoom=15&size=320x160&markers=$lat,$lng,red-pushpin"
        }
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        // combinedClickable, not a plain clickable — see GenericFileBubble's onLongPress doc for
        // why (a bare `clickable` always fires onClick on release regardless of hold duration,
        // swallowing long-press before it can reach a parent's long-click handler). This card
        // also sits as a sibling of — not nested inside — the per-message dropdown-menu Box in
        // MessageFooterContent, so long-pressing it could never have reached that menu anyway;
        // onLongPress instead routes to the same multi-select toggle every other attachment type
        // uses (see delete_selected_message.yaml in .maestro/).
        modifier = Modifier
            .widthIn(min = 160.dp, max = 240.dp)
            .combinedClickable(
                onClick = { openLocationInMaps(context, mapsUrl, latLng) },
                onLongClick = onLongPress,
            ),
    ) {
        Column {
            if (staticMapUrl != null) {
                AsyncImage(
                    model = staticMapUrl,
                    contentDescription = stringResource(R.string.chat_shared_location),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.chat_shared_location),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.chat_view_on_maps),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
internal fun ReadReceiptIcon(isRead: Boolean, unreadTint: Color? = null) {
    Icon(
        imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
        contentDescription = if (isRead) stringResource(R.string.chat_read) else stringResource(R.string.chat_sent),
        modifier = Modifier.size(14.dp),
        tint = if (isRead)
            Color(0xFF4FC3F7)
        else
            unreadTint ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    )
}

/**
 * Pending/failed/sent-or-read status icon shown next to a sent message's time.
 *
 * [neutralTint] overrides the "pending" / "sent but unread" colors only — the read (blue) and
 * failed (error) states stay as-is regardless, since those need to read correctly on any
 * background. Pass a light tint when placing this over a photo (see [MediaMetaOverlay]).
 */
@Composable
internal fun SendStatusIcon(
    sendStatus: SendStatus,
    isRead: Boolean,
    neutralTint: Color? = null,
) {
    when (sendStatus) {
        SendStatus.PENDING ->
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = stringResource(R.string.chat_pending_send),
                tint = neutralTint ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(12.dp),
            )
        SendStatus.FAILED ->
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.chat_send_error),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp),
            )
        SendStatus.SENT ->
            ReadReceiptIcon(isRead = isRead, unreadTint = neutralTint)
    }
}

/**
 * Time (+ edited/self-destruct/status) pill overlaid on the bottom-right corner of a photo or
 * GIF that has no caption — mirrors what [MessageFooterContent]'s trailing row shows for a
 * captioned message, but styled to stay legible over arbitrary image content instead of sitting
 * in a padded strip below it.
 */
@Composable
internal fun MediaMetaOverlay(
    message: MessageBO,
    timeText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(50),
        modifier = modifier.padding(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            message.expiresAt?.let { exp ->
                val secsLeft = ((exp - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                Text(
                    "⏱️ ${if (secsLeft < 60) "${secsLeft}s" else "${secsLeft / 60}m"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
            if (message.isEdited) {
                Text(
                    stringResource(R.string.chat_edited_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
            if (message.isFromMe) {
                SendStatusIcon(
                    sendStatus = message.sendStatus,
                    isRead = message.isRead,
                    neutralTint = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
internal fun ReplyQuote(
    senderName: String,
    content: String,
    isFromMe: Boolean,
    onClick: () -> Unit = {},
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = if (isFromMe)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .widthIn(min = 120.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .height(IntrinsicSize.Min)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxSize()
                .background(accent),
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LinkPreviewCard(data: LinkPreviewData) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(data.url)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
    ) {
        Column {
            if (data.imageUrl != null) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (data.description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = runCatching { java.net.URL(data.url).host }.getOrDefault(data.url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
