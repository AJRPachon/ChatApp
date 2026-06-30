package com.ajrpachon.chatapp.ui.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.components.ConversationListSkeleton
import com.ajrpachon.chatapp.ui.status.StatusBar
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.ConversationListRoute
import com.ajrpachon.chatapp.CreateGroupRoute
import com.ajrpachon.chatapp.InvitationsRoute
import com.ajrpachon.chatapp.NewChatRoute
import com.ajrpachon.chatapp.ProfileRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = ChatRoute::class, label = "Open Chat")
@NavEdge(to = NewChatRoute::class, label = "New Chat")
@NavEdge(to = InvitationsRoute::class, label = "Invitations")
@NavEdge(to = ProfileRoute::class, label = "Profile")
@NavEdge(to = CreateGroupRoute::class, label = "New Group")
@NavDestination(route = ConversationListRoute::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onOpenConversation: (id: String, name: String, isGroup: Boolean) -> Unit,
    onOpenInvitations: () -> Unit,
    onNewChat: () -> Unit,
    onNewGroup: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onOpenStatusViewer: (userId: String) -> Unit = {},
) {
    val vm: ConversationListViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var menuConvId by remember { mutableStateOf<String?>(null) }
    val archivedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is ConversationListEffect.NavigateToChat ->
                    onOpenConversation(effect.conversationId, effect.conversationName, effect.isGroup)
            }
        }
    }

    if (state.showArchivedSheet) {
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(ConversationListIntent.DismissArchivedSheet) },
            sheetState = archivedSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Text(
                text = "Archivados",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.archivedConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No hay chats archivados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(state.archivedConversations, key = { it.id }) { conv ->
                        ArchivedConversationItem(
                            conversation = conv,
                            onClick = dropUnlessResumed {
                                vm.onIntent(ConversationListIntent.DismissArchivedSheet)
                                vm.onIntent(ConversationListIntent.OpenConversation(conv.id, conv.name, conv.isGroup))
                            },
                            onUnarchive = {
                                vm.onIntent(ConversationListIntent.ArchiveConversation(conv.id, false))
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chats",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = { vm.onIntent(ConversationListIntent.ShowArchivedSheet) }) {
                        BadgedBox(badge = {
                            if (state.archivedConversations.isNotEmpty()) {
                                Badge { Text(state.archivedConversations.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Inventory2, contentDescription = "Archivados")
                        }
                    }
                    IconButton(onClick = dropUnlessResumed { onOpenProfile() }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Perfil",
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape,
                                )
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = dropUnlessResumed { onOpenInvitations() }) {
                        BadgedBox(badge = {
                            if (state.pendingInvitationsCount > 0) {
                                Badge { Text(state.pendingInvitationsCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Invitaciones")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = dropUnlessResumed { onNewGroup() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.Group, contentDescription = "Nuevo grupo")
                }
                FloatingActionButton(
                    onClick = dropUnlessResumed { onNewChat() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Nuevo chat")
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            ConversationListSkeleton(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                item(key = "status_bar") {
                    StatusBar(onViewStatus = { status -> onOpenStatusViewer(status.userId) })
                }
                item(key = "sort_filter") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        FilterChip(
                            selected = state.sortByUnread,
                            onClick = { vm.onIntent(ConversationListIntent.ToggleSortByUnread) },
                            label = { Text("No leídos") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }
                if (state.conversations.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No hay conversaciones aún",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "Toca el botón para empezar un chat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
                items(state.conversations, key = { it.id }) { conv ->
                    SwipeableConversationItem(
                        conversation = conv,
                        currentUserId = state.currentUserId,
                        draft = state.drafts[conv.id],
                        showMenu = menuConvId == conv.id,
                        onClick = dropUnlessResumed {
                            vm.onIntent(ConversationListIntent.OpenConversation(conv.id, conv.name, conv.isGroup))
                        },
                        onLongClick = { menuConvId = conv.id },
                        onMenuDismiss = { menuConvId = null },
                        onMuteToggle = {
                            menuConvId = null
                            vm.onIntent(ConversationListIntent.ToggleMute(conv.id, !conv.isMuted))
                        },
                        onClearChat = {
                            menuConvId = null
                            vm.onIntent(ConversationListIntent.ClearChat(conv.id))
                        },
                        onLeaveGroup = if (conv.isGroup) {
                            {
                                menuConvId = null
                                vm.onIntent(ConversationListIntent.LeaveGroup(conv.id))
                            }
                        } else null,
                        onDelete = {
                            menuConvId = null
                            vm.onIntent(ConversationListIntent.DeleteConversation(conv.id))
                        },
                        onArchive = {
                            vm.onIntent(ConversationListIntent.ArchiveConversation(conv.id, true))
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationItem(
    conversation: ConversationBO,
    currentUserId: String?,
    draft: String? = null,
    showMenu: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onMuteToggle: () -> Unit,
    onClearChat: () -> Unit,
    onLeaveGroup: (() -> Unit)?,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onArchive()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = "Archivar",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        "Archivar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        },
    ) {
        ConversationItem(
            conversation = conversation,
            currentUserId = currentUserId,
            draft = draft,
            showMenu = showMenu,
            onClick = onClick,
            onLongClick = onLongClick,
            onMenuDismiss = onMenuDismiss,
            onMuteToggle = onMuteToggle,
            onClearChat = onClearChat,
            onLeaveGroup = onLeaveGroup,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun ArchivedConversationItem(
    conversation: ConversationBO,
    onClick: () -> Unit,
    onUnarchive: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAppAvatar(
            name = conversation.name,
            url = conversation.displayAvatarUrl,
            isGroup = conversation.isGroup,
            size = 48.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            conversation.lastMessage?.let { msg ->
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onUnarchive) {
            Icon(
                Icons.Default.Unarchive,
                contentDescription = "Desarchivar",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: ConversationBO,
    currentUserId: String?,
    draft: String? = null,
    showMenu: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onMuteToggle: () -> Unit,
    onClearChat: () -> Unit,
    onLeaveGroup: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val lastMsg = conversation.lastMessage
    val fromMe = lastMsg?.isFromMe == true
    val hasUnread = conversation.unreadCount > 0

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isOtherUserOnline = !conversation.isGroup &&
                conversation.participants.any { it.id != currentUserId && it.isOnline() }
            Box {
                ChatAppAvatar(
                    name = conversation.name,
                    url = conversation.displayAvatarUrl,
                    isGroup = conversation.isGroup,
                    size = 56.dp,
                )
                if (isOtherUserOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(
                                androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                CircleShape,
                            )
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatConversationTime(conversation.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            !draft.isNullOrBlank() -> Text(
                                text = "Borrador: $draft",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            conversation.trailingImageCount > 0 -> {
                                val label = if (conversation.trailingImageCount == 1) "📷 Foto"
                                else "📷 ${conversation.trailingImageCount} fotos"
                                Text(
                                    text = if (fromMe) "Tú: $label" else label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            lastMsg?.gifUrl != null -> Text(
                                text = if (fromMe) "Tú: GIF" else "GIF",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            lastMsg?.stickerUrl != null -> Text(
                                text = if (fromMe) "Tú: ${lastMsg.stickerUrl}" else lastMsg.stickerUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            lastMsg != null -> Text(
                                text = if (fromMe) "Tú: ${lastMsg.content}" else lastMsg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasUnread)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            else -> Text(
                                "Sin mensajes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (conversation.isMuted) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Silenciado",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                        if (hasUnread) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onMenuDismiss,
        ) {
            DropdownMenuItem(
                text = { Text(if (conversation.isMuted) "Activar notificaciones" else "Silenciar") },
                leadingIcon = {
                    Icon(
                        if (conversation.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                    )
                },
                onClick = onMuteToggle,
            )
            DropdownMenuItem(
                text = { Text("Vaciar chat") },
                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                onClick = onClearChat,
            )
            if (onLeaveGroup != null) {
                DropdownMenuItem(
                    text = { Text("Salir del grupo") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    onClick = onLeaveGroup,
                )
            }
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = onDelete,
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun formatConversationTime(instant: Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(tz)
    val dt = instant.toLocalDateTime(tz)

    return when {
        dt.date == now.date -> {
            val h = dt.hour.toString().padStart(2, '0')
            val m = dt.minute.toString().padStart(2, '0')
            "$h:$m"
        }
        dt.date.year == now.date.year && dt.date.dayOfYear == now.date.dayOfYear - 1 -> "Ayer"
        dt.date.year == now.date.year -> {
            val day = dt.day.toString().padStart(2, '0')
            val month = dt.monthNumber.toString().padStart(2, '0')
            "$day/$month"
        }
        else -> {
            val day = dt.day.toString().padStart(2, '0')
            val month = dt.monthNumber.toString().padStart(2, '0')
            val year = dt.year.toString().takeLast(2)
            "$day/$month/$year"
        }
    }
}
