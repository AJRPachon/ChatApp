package com.ajrpachon.chatapp.ui.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import com.ajrpachon.chatapp.ui.components.InvitationsSkeleton
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.InvitationsRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = ChatRoute::class, label = "Accept Invitation")
@NavDestination(route = InvitationsRoute::class)
@Composable
fun InvitationsScreen(
    onBack: () -> Unit,
    onNavigateToChat: (conversationId: String, name: String) -> Unit = { _, _ -> },
) {
    val vm: InvitationsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is InvitationsEffect.ShowMessage -> snackbar.showSnackbar(effect.text)
                is InvitationsEffect.NavigateToChat -> onNavigateToChat(effect.conversationId, effect.name)
            }
        }
    }

    Scaffold(
        topBar = {
            ChatAppTopBar(title = stringResource(R.string.invitations_top_bar_title), onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // ── Filter chips: Recibidas · N / Enviadas — matches the design canvas.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                FilterChip(
                    text = stringResource(R.string.invitations_filter_received, state.invitations.size),
                    selected = state.selectedTab == InvitationsTab.RECEIVED,
                    onClick = { vm.onIntent(InvitationsIntent.SelectTab(InvitationsTab.RECEIVED)) },
                )
                FilterChip(
                    text = stringResource(R.string.invitations_filter_sent),
                    selected = state.selectedTab == InvitationsTab.SENT,
                    onClick = { vm.onIntent(InvitationsIntent.SelectTab(InvitationsTab.SENT)) },
                )
            }

            when (state.selectedTab) {
                InvitationsTab.RECEIVED -> ReceivedList(
                    isLoading = state.isLoading,
                    invitations = state.invitations,
                    onAccept = { vm.onIntent(InvitationsIntent.Accept(it)) },
                    onReject = { vm.onIntent(InvitationsIntent.Reject(it)) },
                )
                InvitationsTab.SENT -> SentList(
                    isLoading = state.isSentLoading,
                    invitations = state.sentInvitations,
                    onCancel = { vm.onIntent(InvitationsIntent.CancelSent(it)) },
                )
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReceivedList(
    isLoading: Boolean,
    invitations: List<InvitationBO>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    when {
        isLoading -> InvitationsSkeleton()
        invitations.isEmpty() -> EmptyState(stringResource(R.string.invitations_no_pending))
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(invitations, key = { it.id }) { invitation ->
                InvitationRow(
                    avatarName = invitation.sender.displayName,
                    avatarUrl = invitation.sender.avatarUrl,
                    name = invitation.sender.displayName,
                    subtitle = stringResource(R.string.invitations_wants_to_add_you),
                ) {
                    ActionIcon(
                        icon = Icons.Default.Check,
                        contentDescription = stringResource(R.string.invitations_accept_cd),
                        container = MaterialTheme.colorScheme.primary,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onAccept(invitation.id) },
                    )
                    ActionIcon(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.invitations_decline_cd),
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onReject(invitation.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SentList(
    isLoading: Boolean,
    invitations: List<InvitationBO>,
    onCancel: (String) -> Unit,
) {
    when {
        isLoading -> InvitationsSkeleton()
        invitations.isEmpty() -> EmptyState(stringResource(R.string.invitations_no_sent))
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(invitations, key = { it.id }) { invitation ->
                val receiver = invitation.receiver
                InvitationRow(
                    avatarName = receiver?.displayName.orEmpty(),
                    avatarUrl = receiver?.avatarUrl,
                    name = receiver?.displayName.orEmpty(),
                    subtitle = when (invitation.status) {
                        InvitationStatus.PENDING -> stringResource(R.string.newchat_pending_response)
                        InvitationStatus.ACCEPTED -> stringResource(R.string.invitations_sent_accepted)
                        InvitationStatus.REJECTED -> stringResource(R.string.invitations_sent_rejected)
                    },
                ) {
                    if (invitation.status == InvitationStatus.PENDING) {
                        ActionIcon(
                            icon = Icons.Default.Close,
                            contentDescription = stringResource(R.string.invitations_cancel_cd),
                            container = MaterialTheme.colorScheme.surfaceVariant,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onCancel(invitation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * One invitation row: avatar + name/subtitle + trailing actions — matches the design canvas's
 * `.row` (avatar, body, compact square action buttons) instead of the previous stacked
 * name/username + full-width Accept/Reject button pair.
 */
@Composable
private fun InvitationRow(
    avatarName: String,
    avatarUrl: String?,
    name: String,
    subtitle: String,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChatAppAvatar(name = avatarName, url = avatarUrl, size = 46.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    container: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(16.dp))
    }
}
