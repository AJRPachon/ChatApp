package com.ajrpachon.chatapp.ui.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.ui.components.ChatAppAcceptRejectRow
import com.ajrpachon.chatapp.ui.components.InvitationsSkeleton
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.InvitationsRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = ChatRoute::class, label = "Accept Invitation")
@NavDestination(route = InvitationsRoute::class)
@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { Text("Invitaciones") },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        when {
            state.isLoading -> InvitationsSkeleton(modifier = Modifier.padding(innerPadding))

            state.invitations.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No tienes invitaciones pendientes", style = MaterialTheme.typography.bodyLarge)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.invitations, key = { it.id }) { invitation ->
                    InvitationItem(
                        invitation = invitation,
                        onAccept = { vm.onIntent(InvitationsIntent.Accept(invitation.id)) },
                        onReject = { vm.onIntent(InvitationsIntent.Reject(invitation.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationItem(
    invitation: InvitationBO,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = invitation.sender.displayName,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "@${invitation.sender.username}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ChatAppAcceptRejectRow(
            onAccept = onAccept,
            onReject = onReject,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
