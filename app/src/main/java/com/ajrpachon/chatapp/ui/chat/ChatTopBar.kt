package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.ui.theme.IncognitoAccent
import com.ajrpachon.chatapp.ui.theme.IncognitoBannerBackground

/**
 * [ChatScreen]'s `Scaffold.topBar`: the incognito banner, the app bar itself (title/avatar or
 * multi-select count, back/clear-selection navigation, the call/overflow menus), and the
 * pinned-message banner. Extracted per docs/chat-viewmodel-decomposition.md Phase 2.
 *
 * [showDeleteSelectionConfirm] is [MutableState] because [ChatDialogHost] (rendered elsewhere in
 * ChatScreen) owns the confirmation dialog this bar's delete action triggers — same reasoning as
 * ChatDialogHost's own doc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTopBar(
    state: ChatState,
    vm: ChatViewModel,
    latestPinned: MessageBO?,
    pinnedBannerVisible: MutableState<Boolean>,
    showDeleteSelectionConfirm: MutableState<Boolean>,
    onBack: () -> Unit,
    onGroupInfo: () -> Unit,
    onUserInfo: (userId: String) -> Unit,
    onOpenMediaGallery: () -> Unit,
    onScrollToMessage: (String) -> Unit,
) {
    Column {
        if (state.incognito.isIncognito) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IncognitoBannerBackground)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.chat_incognito_banner),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
        if (state.isMultiSelectActive) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.onIntent(ChatIntent.ClearSelection) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_cancel_selection))
                    }
                },
                title = { Text(stringResource(R.string.chat_selected_count, state.selectedMessageIds.size)) },
                actions = {
                    IconButton(onClick = { vm.onIntent(ChatIntent.ShowForwardSelectionDialog) }) {
                        Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = stringResource(R.string.chat_forward_selected))
                    }
                    IconButton(onClick = { showDeleteSelectionConfirm.value = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.chat_delete_selected))
                    }
                },
            )
        } else TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = dropUnlessResumed {
                            if (state.isGroup) onGroupInfo()
                            else state.otherUserId?.let { onUserInfo(it) }
                        })
                        .padding(vertical = 4.dp),
                ) {
                    val avatarUrl = if (state.isGroup) state.groupAvatarUrl else state.otherUserAvatarUrl
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = stringResource(R.string.chat_photo_of, state.conversationTitle),
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isGroup) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                Text(
                                    text = state.conversationTitle.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.conversationTitle.ifBlank { stringResource(R.string.chat_title_default) },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee(iterations = 1),
                            )
                            if (state.disappearing.seconds > 0L) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = stringResource(R.string.chat_disappearing_mode_active),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                        val secondaryLine = state.presenceText ?: state.subtitleText
                        secondaryLine?.let { line ->
                            val isActive = state.isOtherUserOnline || state.groupPresence.onlineMemberCount > 0
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = if (isActive)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = dropUnlessResumed { onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.chat_back))
                }
            },
            actions = {
                if (state.scheduling.messageCount > 0) {
                    Box {
                        IconButton(onClick = { vm.onIntent(ChatIntent.ShowScheduledSheet) }) {
                            Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.chat_scheduled_messages))
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.scheduling.messageCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
                if (state.isGroup || state.otherUserId != null) {
                    var callMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { callMenuExpanded = true }) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = stringResource(R.string.chat_call))
                        }
                        DropdownMenu(
                            expanded = callMenuExpanded,
                            onDismissRequest = { callMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (state.isGroup) stringResource(R.string.chat_group_voice_call)
                                        else stringResource(R.string.chat_voice_call)
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                onClick = {
                                    callMenuExpanded = false
                                    vm.onIntent(ChatIntent.StartCall("audio"))
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (state.isGroup) stringResource(R.string.chat_group_video_call)
                                        else stringResource(R.string.chat_video_call)
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                                onClick = {
                                    callMenuExpanded = false
                                    vm.onIntent(ChatIntent.StartCall("video"))
                                },
                            )
                        }
                    }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.chat_more_options))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_search_messages)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                vm.onIntent(ChatIntent.OpenSearch)
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (state.mute.isMuted) stringResource(R.string.chat_enable_notifications) else stringResource(R.string.chat_mute))
                            },
                            leadingIcon = {
                                Icon(
                                    if (state.mute.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                if (state.mute.isMuted) {
                                    vm.onIntent(ChatIntent.MuteFor(0L))
                                } else {
                                    vm.onIntent(ChatIntent.ShowMuteDialog)
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_chat_theme)) },
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                vm.onIntent(ChatIntent.OpenThemePicker)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_wallpaper)) },
                            leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                vm.onIntent(ChatIntent.OpenWallpaperPicker)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_export_conversation)) },
                            leadingIcon = {
                                if (state.isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            },
                            enabled = !state.isExporting,
                            onClick = {
                                menuExpanded = false
                                vm.onIntent(ChatIntent.ExportConversation)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_disappearing_mode)) },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                vm.onIntent(ChatIntent.ShowDisappearingModeSheet)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.incognito.isIncognito) stringResource(R.string.chat_disable_incognito) else stringResource(R.string.chat_incognito_mode)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (state.incognito.isIncognito) IncognitoAccent else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                vm.onIntent(ChatIntent.ToggleIncognito)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_shared_media)) },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenMediaGallery()
                            },
                        )
                        if (state.isGroup) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_group_info)) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onGroupInfo()
                                },
                            )
                            if (state.isCurrentUserMember) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_leave_group), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        vm.onIntent(ChatIntent.LeaveGroup)
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
        if (latestPinned != null && pinnedBannerVisible.value) {
            PinnedMessageBanner(
                message = latestPinned,
                pinnedCount = state.pinnedMessages.size,
                onTap = {
                    onScrollToMessage(latestPinned.id)
                },
                onHide = { pinnedBannerVisible.value = false },
                onDismiss = { vm.onIntent(ChatIntent.UnpinMessage(latestPinned.id)) },
            )
        }
    }
}
