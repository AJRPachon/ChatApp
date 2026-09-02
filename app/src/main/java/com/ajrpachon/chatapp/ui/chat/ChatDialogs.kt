package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.ConversationBO

// ── Simple modal dialogs used from ChatScreen ───────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — each takes only its own narrow
// params, no dependency on ChatViewModel or the message-bubble rendering tree.

@Composable
internal fun ExpiryDurationDialog(onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    val options = listOf(
        stringResource(R.string.chat_expiry_1_minute) to (System.currentTimeMillis() + 60_000L),
        stringResource(R.string.chat_1_hour) to (System.currentTimeMillis() + 3_600_000L),
        stringResource(R.string.chat_24_hours) to (System.currentTimeMillis() + 86_400_000L),
        stringResource(R.string.chat_7_days) to (System.currentTimeMillis() + 604_800_000L),
        stringResource(R.string.chat_remove_self_destruct) to null,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_ephemeral_message)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    stringResource(R.string.chat_ephemeral_message_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(8.dp))
                options.forEach { (label, value) ->
                    TextButton(
                        onClick = { onSelect(value) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    ) {
                        Text(label, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
internal fun MuteDurationDialog(onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val options = listOf(
        stringResource(R.string.chat_1_hour) to (System.currentTimeMillis() + 3_600_000L),
        stringResource(R.string.chat_8_hours) to (System.currentTimeMillis() + 28_800_000L),
        stringResource(R.string.chat_24_hours) to (System.currentTimeMillis() + 86_400_000L),
        stringResource(R.string.chat_mute_always) to -1L,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_mute_notifications_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                options.forEach { (label, value) ->
                    TextButton(
                        onClick = { onSelect(value) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    ) {
                        Text(label, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
internal fun ForwardConversationDialog(
    conversations: List<ConversationBO>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_forward_to)) },
        text = {
            if (conversations.isEmpty()) {
                Text(
                    stringResource(R.string.chat_no_other_conversations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(conversations, key = { _, c -> c.id }) { _, conv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(conv.id) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val avatarUrl = conv.displayAvatarUrl
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (conv.isGroup) {
                                        Icon(
                                            Icons.Default.Group,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    } else {
                                        Text(
                                            text = conv.name.firstOrNull()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = conv.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 60_000,
    )
    val timePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) + 5,
        is24Hour = true,
    )
    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) {
                    Text(stringResource(R.string.chat_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.chat_select_time)) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMs = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = selectedDateMs
                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onConfirm(cal.timeInMillis)
                }) {
                    Text(stringResource(R.string.chat_schedule_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ImageViewerDialog(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex),
        ) { imageUrls.size }

        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // One photo per page, always centered (Fit + fillMaxSize letterboxes instead of
            // pinning content to the top) — swipe between photos instead of stacking them in a
            // scrollable column.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = stringResource(R.string.userinfo_fullscreen_image_cd),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (imageUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 4.dp, end = 8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_close), tint = Color.White)
            }
        }
    }
}
