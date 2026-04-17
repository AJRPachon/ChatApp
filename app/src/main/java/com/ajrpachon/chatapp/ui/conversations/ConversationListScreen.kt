package com.ajrpachon.chatapp.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.ajrpachon.chatapp.domain.model.ConversationBO
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onOpenConversation: (String) -> Unit,
    onOpenInvitations: () -> Unit,
    pendingInvitations: Int = 0,
) {
    val vm: ConversationListViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is ConversationListEffect.NavigateToChat -> onOpenConversation(effect.conversationId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = {
                    IconButton(onClick = dropUnlessResumed { onOpenInvitations() }) {
                        BadgedBox(badge = {
                            if (pendingInvitations > 0) Badge { Text(pendingInvitations.toString()) }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Invitaciones")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                items(state.conversations, key = { it.id }) { conv ->
                    ConversationItem(
                        conversation = conv,
                        onClick = dropUnlessResumed {
                            vm.onIntent(ConversationListIntent.OpenConversation(conv.id))
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(conversation: ConversationBO, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(conversation.name) },
        supportingContent = {
            Text(conversation.lastMessage?.content ?: "Sin mensajes")
        },
        trailingContent = {
            if (conversation.unreadCount > 0) {
                Badge { Text(conversation.unreadCount.toString()) }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
