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
import androidx.compose.material3.TextButton
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
    vm: ChatViewModel,
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
                TextButton(onClick = {
                    showDeleteSelectionConfirm.value = false
                    vm.onIntent(ChatIntent.DeleteSelectedMessages)
                }) { Text(stringResource(R.string.chat_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectionConfirm.value = false }) { Text(stringResource(R.string.chat_cancel)) }
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
            onDismiss = { vm.onIntent(ChatIntent.DismissExpiryDialog) },
            onSelect = { vm.onIntent(ChatIntent.SetExpiry(msgId, it)) },
        )
    }

    if (state.showForwardDialog) {
        ForwardConversationDialog(
            conversations = state.forwardableConversations,
            onDismiss = { vm.onIntent(ChatIntent.DismissForwardDialog) },
            onSelect = { targetConversationId ->
                val forwardingMsg = state.forwardingMessage
                if (forwardingMsg != null) {
                    vm.onIntent(ChatIntent.ForwardMessage(forwardingMsg.id, targetConversationId))
                } else {
                    vm.onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
                }
            },
        )
    }

    if (state.showIncognitoInfoDialog) {
        AlertDialog(
            onDismissRequest = { vm.onIntent(ChatIntent.DismissIncognitoDialog) },
            title = { Text(stringResource(R.string.chat_incognito_mode)) },
            text = {
                Text(stringResource(R.string.chat_incognito_mode_description))
            },
            confirmButton = {
                TextButton(onClick = { vm.onIntent(ChatIntent.ConfirmIncognito) }) {
                    Text(stringResource(R.string.chat_incognito_confirm_activate))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onIntent(ChatIntent.DismissIncognitoDialog) }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }

    // Was previously duplicated verbatim (two identical `if` blocks, so this dialog composed
    // twice, stacked, whenever true) — collapsed to one during the Phase 2 scaffold split.
    if (state.showForwardSelectionDialog) {
        ForwardConversationDialog(
            conversations = state.forwardableConversations,
            onDismiss = { vm.onIntent(ChatIntent.DismissForwardSelectionDialog) },
            onSelect = { targetConversationId ->
                vm.onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
            },
        )
    }

    if (state.showMuteDialog) {
        MuteDurationDialog(
            onDismiss = { vm.onIntent(ChatIntent.DismissMuteDialog) },
            onSelect = { vm.onIntent(ChatIntent.MuteFor(it)) },
        )
    }

    var showStickerStore by remember { mutableStateOf(false) }

    if (state.showStickerPicker) {
        StickerGifPicker(
            onStickerSelected = { vm.onIntent(ChatIntent.SendSticker(it)) },
            onGifSelected = { vm.onIntent(ChatIntent.SendGif(it)) },
            onOpenStore = {
                vm.onIntent(ChatIntent.CloseStickerPicker)
                showStickerStore = true
            },
            onDismiss = { vm.onIntent(ChatIntent.CloseStickerPicker) },
        )
    }

    if (showStickerStore) {
        StickerStoreSheet(onDismiss = { showStickerStore = false })
    }

    val chatTheme = state.chatTheme

    if (state.showThemePicker) {
        ChatThemePickerSheet(
            currentTheme = chatTheme,
            onSelect = { vm.onIntent(ChatIntent.SetChatTheme(it)) },
            onDismiss = { vm.onIntent(ChatIntent.DismissThemePicker) },
        )
    }

    if (state.showDisappearingModeSheet) {
        DisappearingModeSheet(
            currentSeconds = state.disappearingModeSeconds,
            onDismiss = { vm.onIntent(ChatIntent.DismissDisappearingModeSheet) },
            onSelect = { seconds -> vm.onIntent(ChatIntent.SetDisappearingMode(conversationId, seconds)) },
        )
    }

    if (state.scheduling.showDialog) {
        ScheduleMessageDialog(
            onDismiss = { vm.onIntent(ChatIntent.DismissScheduleDialog) },
            onConfirm = { scheduledAtMs -> vm.onIntent(ChatIntent.ScheduleMessage(scheduledAtMs)) },
        )
    }

    if (state.scheduling.showSheet) {
        val scheduledSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(ChatIntent.DismissScheduledSheet) },
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
                            IconButton(onClick = { vm.onIntent(ChatIntent.CancelScheduledMessage(msg.id)) }) {
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
            onDismiss = { vm.onIntent(ChatIntent.DismissAiSheet) },
            onSuggestReply = { vm.onIntent(ChatIntent.AiSuggestReply) },
            onFreeform = { prompt -> vm.onIntent(ChatIntent.AiFreeform(prompt)) },
            onInsert = { vm.onIntent(ChatIntent.InsertAiSuggestion) },
        )
    }

    if (state.poll.showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(ChatIntent.DismissCreatePollSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CreatePollSheetContent(
                onDismiss = { vm.onIntent(ChatIntent.DismissCreatePollSheet) },
                onCreate = { question, options, allowMultiple ->
                    vm.onIntent(ChatIntent.CreatePoll(question, options, allowMultiple))
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

    if (state.showWallpaperPicker) {
        WallpaperPickerSheet(
            currentColor = state.wallpaperColor,
            onSelect = { colorValue: Long? ->
                vm.onIntent(ChatIntent.SetWallpaperColor(colorValue))
                vm.onIntent(ChatIntent.DismissWallpaperPicker)
            },
            onDismiss = { vm.onIntent(ChatIntent.DismissWallpaperPicker) },
        )
    }
}
