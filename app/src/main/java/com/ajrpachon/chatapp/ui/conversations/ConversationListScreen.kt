package com.ajrpachon.chatapp.ui.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.components.ConversationListSkeleton
import kotlinx.datetime.Instant
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
) {
    val vm: ConversationListViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var menuConvId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is ConversationListEffect.NavigateToChat ->
                    onOpenConversation(effect.conversationId, effect.conversationName, effect.isGroup)
            }
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
        } else if (state.conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                items(state.conversations, key = { it.id }) { conv ->
                    ConversationItem(
                        conversation = conv,
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
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: ConversationBO,
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
            ChatAppAvatar(
                name = conversation.name,
                url = conversation.displayAvatarUrl,
                isGroup = conversation.isGroup,
                size = 56.dp,
            )

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
                                Icons.Default.VolumeOff,
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
