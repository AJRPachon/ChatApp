package com.ajrpachon.chatapp.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.utils.LinkPreviewData
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Message-bubble rendering tree ───────────────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — the higher-risk group left for
// a later pass after the self-contained dialogs/sheets. This file holds the hub (MessageBubble,
// the per-message-type dispatcher) plus the two building blocks nearly every bubble type in the
// other bubble files renders through: ReplySelectContainer (swipe-to-reply + multi-select
// overlay) and ChatBubbleSlot (the outer-margin width rule).

/**
 * Cheap, pure key extraction shared between MessageBubble's own content-type branching and its
 * call site in ChatScreen (which uses these to narrow the whole-conversation pollUiStates/
 * contactPhoneLookups maps down to just the single entry a given message needs, before passing
 * them down — a MessageBubble instance should only recompose when the poll/contact data IT
 * depends on changes, not whenever any other message's poll/contact data changes elsewhere in
 * the map).
 */
internal fun pollIdOf(content: String): String? =
    content.takeIf { it.startsWith("poll:") }?.removePrefix("poll:")

internal fun contactPhoneOf(content: String): String? {
    if (!content.startsWith("contact:{")) return null
    val json = content.removePrefix("contact:")
    return runCatching { org.json.JSONObject(json).optString("phone", "") }.getOrNull()
}

@Composable
internal fun ReplySelectContainer(
    isFromMe: Boolean,
    isSelected: Boolean,
    isMultiSelectActive: Boolean,
    onToggleSelect: () -> Unit,
    onReply: () -> Unit,
    content: @Composable () -> Unit,
) {
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isMultiSelectActive) onToggleSelect() },
                onLongClick = { onToggleSelect() },
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (!isMultiSelectActive && swipeOffset.value >= swipeThreshold) onReply()
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
        val iconAlpha = (swipeOffset.value / swipeThreshold).coerceIn(0f, 1f)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha(iconAlpha),
        )
        Box(modifier = Modifier.graphicsLayer { translationX = swipeOffset.value }) {
            content()
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.chat_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(if (isFromMe) Alignment.TopEnd else Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp),
            )
        }
    }
}

// ── Chat bubble outer-margin rule ──────────────────────────────────────────────

/** Fraction of the available width a chat bubble may ever occupy. This is the one rule every
 *  bubble type in this file renders through via [ChatBubbleSlot]: a bubble is pinned to the
 *  outer edge — right for messages I sent, left for messages I received — leaving at least a
 *  matching blank margin on the opposite ("inner") side, like a voice-message-style layout.
 *  Bubbles whose natural content is already narrower than the cap simply keep their own width. */
private const val BUBBLE_MAX_WIDTH_FRACTION = 0.85f

/** Single choke point for the outer-margin rule above. Wrap any chat bubble's root content in
 *  this instead of a bare `Modifier.fillMaxWidth()` Row/Column so every bubble type stays
 *  consistent. [content] receives the resolved max width to apply (typically via
 *  `Modifier.widthIn(max = it)`, combined with any smaller fixed cap the bubble already has). */
@Composable
internal fun ChatBubbleSlot(
    isFromMe: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (maxBubbleWidth: Dp) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * BUBBLE_MAX_WIDTH_FRACTION
        Box(modifier = Modifier.align(if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart)) {
            content(maxBubbleWidth)
        }
    }
}

// ── MessageBubble ─────────────────────────────────────────────────────────────

@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList", "ReturnCount")
@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun MessageBubble(
    message: MessageBO,
    isGroup: Boolean = false,
    onImageClick: (String) -> Unit,
    onReply: () -> Unit,
    isHighlighted: Boolean = false,
    onReplyClick: (String) -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onSelfDestruct: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    messageReactions: List<com.ajrpachon.chatapp.domain.model.ReactionBO> = emptyList(),
    currentUserId: String? = null,
    onToggleReaction: (String) -> Unit = {},
    isSelected: Boolean = false,
    isMultiSelectActive: Boolean = false,
    onToggleSelect: () -> Unit = {},
    outgoingBubbleColor: Color = Color.Unspecified,
    onOpenPdf: (url: String, filename: String) -> Unit = { _, _ -> },
    onVote: ((optionId: String) -> Unit)? = null,
    onShowReactionDetails: () -> Unit = {},
    onRetryMessage: (String) -> Unit = {},
    onCopy: (String) -> Unit = {},
    contactPhoneLookups: Map<String, ContactPhoneLookup> = emptyMap(),
    onCheckContactRelationship: (String) -> Unit = {},
    onContactCardPrimaryAction: (String) -> Unit = {},
    pollUiStates: Map<String, PollUiState> = emptyMap(),
    onObservePoll: (String) -> Unit = {},
    linkPreviews: Map<String, LinkPreviewData?> = emptyMap(),
    onDetectedUrl: (String) -> Unit = {},
    translatedText: String? = null,
    isTranslating: Boolean = false,
    onTranslate: () -> Unit = {},
    onDismissTranslation: () -> Unit = {},
) {
    if (message.isDeleted) {
        DeletedMessageBubble(message)
        return
    }
    if (message.isCallMessage) {
        ReplySelectContainer(message.isFromMe, isSelected, isMultiSelectActive, onToggleSelect, onReply) {
            CallMessageBubble(message)
        }
        return
    }
    if (message.stickerUrl != null) {
        ReplySelectContainer(message.isFromMe, isSelected, isMultiSelectActive, onToggleSelect, onReply) {
            StickerBubble(message)
        }
        return
    }
    if (message.content.startsWith("contact:{")) {
        val json = message.content.removePrefix("contact:")
        val contactName = runCatching { org.json.JSONObject(json).getString("name") }.getOrNull()
        val contactPhone = contactPhoneOf(message.content)
        if (contactName != null) {
            ReplySelectContainer(message.isFromMe, isSelected, isMultiSelectActive, onToggleSelect, onReply) {
                ContactBubble(
                    name = contactName,
                    phone = contactPhone ?: "",
                    isFromMe = message.isFromMe,
                    lookup = contactPhoneLookups[contactPhone ?: ""],
                    onCheckRelationship = onCheckContactRelationship,
                    onPrimaryAction = onContactCardPrimaryAction,
                )
            }
        }
        return
    }
    if (message.fileUrl != null) {
        ReplySelectContainer(message.isFromMe, isSelected, isMultiSelectActive, onToggleSelect, onReply) {
            FileBubble(message, onOpenPdf, onLongPress = onToggleSelect)
        }
        return
    }
    if (message.videoUrl != null) {
        ReplySelectContainer(message.isFromMe, isSelected, isMultiSelectActive, onToggleSelect, onReply) {
            VideoBubble(message, onLongPress = onToggleSelect)
        }
        return
    }
    if (message.content.startsWith("poll:")) {
        val pollId = remember(message.content) { pollIdOf(message.content) ?: "" }
        ReplySelectContainer(message.isFromMe, isSelected, isMultiSelectActive, onToggleSelect, onReply) {
            PollBubble(
                pollId = pollId,
                isFromMe = message.isFromMe,
                pollUiState = pollUiStates[pollId],
                onVote = { optionId -> onVote?.invoke(optionId) },
                onObserve = onObservePoll,
            )
        }
        return
    }
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 0.25f else 0f,
        animationSpec = tween(300),
        label = "highlight",
    )
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isMultiSelectActive) onToggleSelect() },
                onLongClick = { onToggleSelect() },
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (!isMultiSelectActive && swipeOffset.value >= swipeThreshold) onReply()
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onDragCancel = {
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val next = swipeOffset.value + dragAmount
                            swipeOffset.snapTo(next.coerceIn(0f, swipeThreshold * 1.3f))
                        }
                    },
                )
            },
    ) {
        val iconAlpha = (swipeOffset.value / swipeThreshold).coerceIn(0f, 1f)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha(iconAlpha),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value },
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
        ) {
        ChatBubbleSlot(isFromMe = message.isFromMe) { maxBubbleWidth ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (message.isFromMe) {
                    if (outgoingBubbleColor != Color.Unspecified) outgoingBubbleColor
                    else MaterialTheme.colorScheme.primaryContainer
                } else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = maxBubbleWidth),
            ) {
                val hasMedia = message.imageUrl != null || message.gifUrl != null
                val hasHeader = (isGroup && !message.isFromMe && message.senderName.isNotBlank()) ||
                    message.replyToId != null
                // A bare photo/GIF (no caption, no audio) bleeds to the bubble's own edges —
                // clipped by Surface's shape — instead of sitting inside the same padded column
                // as text, so it reads as a photo, not a text bubble with a picture stuffed in
                // it. Its time/status moves onto the image itself (see MediaMetaOverlay).
                val mediaIsStandalone = hasMedia && message.content.isBlank() && message.audioUrl == null

                @Composable
                fun BubbleHeader() {
                    if (isGroup && !message.isFromMe && message.senderName.isNotBlank()) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (message.replyToId != null) {
                        ReplyQuote(
                            senderName = message.replyToSenderName ?: "",
                            content = message.replyToContent ?: "",
                            isFromMe = message.isFromMe,
                            onClick = { onReplyClick(message.replyToId) },
                        )
                        if (!hasMedia) Spacer(Modifier.height(6.dp))
                    }
                }

                if (!hasMedia) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        BubbleHeader()
                        MessageFooterContent(
                            message = message,
                            timeText = timeText,
                            onEdit = onEdit,
                            onSelfDestruct = onSelfDestruct,
                            onDelete = onDelete,
                            onForward = onForward,
                            onCopy = onCopy,
                            onToggleReaction = onToggleReaction,
                            linkPreviews = linkPreviews,
                            onDetectedUrl = onDetectedUrl,
                            translatedText = translatedText,
                            isTranslating = isTranslating,
                            onTranslate = onTranslate,
                            onDismissTranslation = onDismissTranslation,
                            onToggleSelect = onToggleSelect,
                        )
                    }
                } else {
                    Column {
                        if (hasHeader) {
                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                                BubbleHeader()
                            }
                        }
                        message.imageUrl?.let { imageUrl ->
                            Box {
                                AsyncImage(
                                    model = imageUrl.takeIf { MediaUrlValidator.isValid(it) },
                                    contentDescription = stringResource(R.string.chat_image_content_description),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        // A bare photo fills the whole bubble (mediaIsStandalone),
                                        // so its own click target is the only surface a user can
                                        // press — a plain `clickable` here would silently swallow
                                        // long-presses too (Compose fires onClick regardless of
                                        // hold duration when no onLongClick is registered on the
                                        // same node), leaving no way to long-press-to-select a
                                        // photo message for bulk delete, unlike every other
                                        // message type. `combinedClickable` restores that parity.
                                        .combinedClickable(
                                            onClick = { onImageClick(imageUrl) },
                                            onLongClick = { onToggleSelect() },
                                        ),
                                )
                                if (mediaIsStandalone) {
                                    MediaMetaOverlay(
                                        message = message,
                                        timeText = timeText,
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                        message.gifUrl?.let { gifUrl ->
                            Box {
                                AsyncImage(
                                    model = gifUrl.takeIf { MediaUrlValidator.isValid(it) },
                                    contentDescription = stringResource(R.string.chat_gif_content_description),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp),
                                )
                                if (mediaIsStandalone) {
                                    MediaMetaOverlay(
                                        message = message,
                                        timeText = timeText,
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                        if (!mediaIsStandalone) {
                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)) {
                                MessageFooterContent(
                                    message = message,
                                    timeText = timeText,
                                    onEdit = onEdit,
                                    onSelfDestruct = onSelfDestruct,
                                    onDelete = onDelete,
                                    onForward = onForward,
                                    onCopy = onCopy,
                                    onToggleReaction = onToggleReaction,
                                    linkPreviews = linkPreviews,
                                    onDetectedUrl = onDetectedUrl,
                                    translatedText = translatedText,
                                    isTranslating = isTranslating,
                                    onTranslate = onTranslate,
                                    onDismissTranslation = onDismissTranslation,
                                    onToggleSelect = onToggleSelect,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (messageReactions.isNotEmpty()) {
            val grouped = messageReactions.groupBy { it.emoji }
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                grouped.forEach { (emoji, reactors) ->
                    val isMine = reactors.any { it.userId == currentUserId }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isMine) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.combinedClickable(
                            onClick = { onToggleReaction(emoji) },
                            onLongClick = { onShowReactionDetails() },
                        ),
                    ) {
                        Text(
                            text = "$emoji ${reactors.size}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        } // close Column
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.chat_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(if (message.isFromMe) Alignment.TopEnd else Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp),
            )
        }
    }
}
