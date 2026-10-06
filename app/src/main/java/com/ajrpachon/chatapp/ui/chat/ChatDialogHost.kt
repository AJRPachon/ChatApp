package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.ui.components.ChatAppTextButton

/**
 * Every dialog/bottom-sheet that [ChatScreen] shows conditionally on a `state.showX` (or local
 * one-off) flag, in one place, instead of ~20 independent `if` blocks inline in the screen's own
 * body. Extracted per docs/chat-viewmodel-decomposition.md Phase 2.
 *
 * [showDeleteSelectionConfirm]/[showViewer]/[viewerUrls]/[viewerInitialIndex]/
 * [reactionDetailMessageId] are [MutableState] rather than plain values + callbacks because
 * ChatScreen's own message-list content also reads and writes them (opening the image viewer or
 * the reaction-details sheet from a tapped bubble) — passing the state holder directly keeps
 * both sides in sync without threading extra get/set params through.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatDialogHost(
    state: ChatState,
    onIntent: (ChatIntent) -> Unit,
    conversationId: String,
    reactions: Map<String, List<ReactionBO>>,
    showDeleteSelectionConfirm: MutableState<Boolean>,
    showViewer: MutableState<Boolean>,
    viewerUrls: MutableState<List<String>>,
    viewerInitialIndex: MutableState<Int>,
    reactionDetailMessageId: MutableState<String?>,
) {
    if (showDeleteSelectionConfirm.value) {
        val count = state.selectedMessageIds.size
        AlertDialog(
            onDismissRequest = { showDeleteSelectionConfirm.value = false },
            title = { Text(stringResource(R.string.chat_delete_messages_title)) },
            text = { Text(pluralStringResource(R.plurals.chat_delete_messages_confirm, count, count)) },
            confirmButton = {
                ChatAppTextButton(
                    text = stringResource(R.string.chat_delete),
                    onClick = {
                        showDeleteSelectionConfirm.value = false
                        onIntent(ChatIntent.DeleteSelectedMessages)
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            },
            dismissButton = {
                ChatAppTextButton(text = stringResource(R.string.chat_cancel), onClick = { showDeleteSelectionConfirm.value = false })
            },
        )
    }

    if (showViewer.value && viewerUrls.value.isNotEmpty()) {
        ImageViewerDialog(
            imageUrls = viewerUrls.value,
            initialIndex = viewerInitialIndex.value,
            onDismiss = { showViewer.value = false },
        )
    }

    state.expiryDialogMessageId?.let { msgId ->
        ExpiryDurationDialog(
            onDismiss = { onIntent(ChatIntent.DismissExpiryDialog) },
            onSelect = { onIntent(ChatIntent.SetExpiry(msgId, it)) },
        )
    }

    if (state.forward.showDialog) {
        ForwardConversationDialog(
            conversations = state.forward.conversations,
            onDismiss = { onIntent(ChatIntent.DismissForwardDialog) },
            onSelect = { targetConversationId ->
                val forwardingMsg = state.forward.message
                if (forwardingMsg != null) {
                    onIntent(ChatIntent.ForwardMessage(forwardingMsg.id, targetConversationId))
                } else {
                    onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
                }
            },
        )
    }

    if (state.incognito.showInfoDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(ChatIntent.DismissIncognitoDialog) },
            title = { Text(stringResource(R.string.chat_incognito_mode)) },
            text = {
                Text(stringResource(R.string.chat_incognito_mode_description))
            },
            confirmButton = {
                ChatAppTextButton(text = stringResource(R.string.chat_incognito_confirm_activate), onClick = { onIntent(ChatIntent.ConfirmIncognito) })
            },
            dismissButton = {
                ChatAppTextButton(text = stringResource(R.string.chat_cancel), onClick = { onIntent(ChatIntent.DismissIncognitoDialog) })
            },
        )
    }

    // Was previously duplicated verbatim (two identical `if` blocks, so this dialog composed
    // twice, stacked, whenever true) — collapsed to one during the Phase 2 scaffold split.
    if (state.forward.showSelectionDialog) {
        ForwardConversationDialog(
            conversations = state.forward.conversations,
            onDismiss = { onIntent(ChatIntent.DismissForwardSelectionDialog) },
            onSelect = { targetConversationId ->
                onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
            },
        )
    }

    if (state.mute.showDialog) {
        MuteDurationDialog(
            onDismiss = { onIntent(ChatIntent.DismissMuteDialog) },
            onSelect = { onIntent(ChatIntent.MuteFor(it)) },
        )
    }

    var showStickerStore by remember { mutableStateOf(false) }

    if (state.showStickerPicker) {
        StickerGifPicker(
            onStickerSelected = { onIntent(ChatIntent.SendSticker(it)) },
            onGifSelected = { onIntent(ChatIntent.SendGif(it)) },
            onOpenStore = {
                onIntent(ChatIntent.CloseStickerPicker)
                showStickerStore = true
            },
            onDismiss = { onIntent(ChatIntent.CloseStickerPicker) },
        )
    }

    if (showStickerStore) {
        StickerStoreSheet(onDismiss = { showStickerStore = false })
    }

    val chatTheme = state.theme.theme

    if (state.theme.showPicker) {
        ChatThemePickerSheet(
            currentTheme = chatTheme,
            onSelect = { onIntent(ChatIntent.SetChatTheme(it)) },
            onDismiss = { onIntent(ChatIntent.DismissThemePicker) },
        )
    }

    if (state.disappearing.showSheet) {
        DisappearingModeSheet(
            currentSeconds = state.disappearing.seconds,
            onDismiss = { onIntent(ChatIntent.DismissDisappearingModeSheet) },
            onSelect = { seconds -> onIntent(ChatIntent.SetDisappearingMode(conversationId, seconds)) },
        )
    }

    if (state.scheduling.showDialog) {
        ScheduleMessageDialog(
            onDismiss = { onIntent(ChatIntent.DismissScheduleDialog) },
            onConfirm = { scheduledAtMs -> onIntent(ChatIntent.ScheduleMessage(scheduledAtMs)) },
        )
    }

    if (state.scheduling.showSheet) {
        val scheduledSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(ChatIntent.DismissScheduledSheet) },
            sheetState = scheduledSheetState,
        ) {
            Text(
                text = stringResource(R.string.chat_scheduled_messages),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.scheduling.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.chat_no_scheduled_messages),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(state.scheduling.messages, key = { it.id }) { msg ->
                        val formatter = remember { java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatter.format(java.util.Date(msg.scheduledAtMs)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onIntent(ChatIntent.CancelScheduledMessage(msg.id)) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.chat_cancel_scheduled_message))
                            }
                        }
                    }
                }
            }
            Spacer(
                Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            )
        }
    }

    if (state.ai.showSheet) {
        AiAssistantSheet(
            aiSuggestion = state.ai.suggestion,
            isAiLoading = state.ai.isLoading,
            onDismiss = { onIntent(ChatIntent.DismissAiSheet) },
            onSuggestReply = { onIntent(ChatIntent.AiSuggestReply) },
            onFreeform = { prompt -> onIntent(ChatIntent.AiFreeform(prompt)) },
            onInsert = { onIntent(ChatIntent.InsertAiSuggestion) },
        )
    }

    if (state.poll.showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(ChatIntent.DismissCreatePollSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CreatePollSheetContent(
                onDismiss = { onIntent(ChatIntent.DismissCreatePollSheet) },
                onCreate = { question, options, allowMultiple ->
                    onIntent(ChatIntent.CreatePoll(question, options, allowMultiple))
                },
            )
        }
    }

    reactionDetailMessageId.value?.let { msgId ->
        val msgReactions = reactions[msgId] ?: emptyList()
        if (msgReactions.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { reactionDetailMessageId.value = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                ReactionDetailsSheet(
                    reactions = msgReactions,
                    onDismiss = { reactionDetailMessageId.value = null },
                )
            }
        }
    }

    if (state.wallpaper.showPicker) {
        WallpaperPickerSheet(
            currentColor = state.wallpaper.color,
            onSelect = { colorValue: Long? ->
                onIntent(ChatIntent.SetWallpaperColor(colorValue))
                onIntent(ChatIntent.DismissWallpaperPicker)
            },
            onDismiss = { onIntent(ChatIntent.DismissWallpaperPicker) },
        )
    }
}
