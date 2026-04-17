package com.ajrpachon.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.ajrpachon.chatapp.ui.auth.AuthScreen
import com.ajrpachon.chatapp.ui.chat.ChatScreen
import com.ajrpachon.chatapp.ui.conversations.ConversationListScreen
import com.ajrpachon.chatapp.ui.invitations.InvitationsScreen
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme

// ── Routes ─────────────────────────────────────────────────────────────────

data object AuthRoute
data object ConversationListRoute
data class ChatRoute(val conversationId: String)
data object InvitationsRoute

// ── Activity ───────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ChatAppTheme {
                val backStack = remember { mutableStateListOf<Any>(AuthRoute) }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { key ->
                        when (key) {
                            is AuthRoute -> NavEntry(key) {
                                AuthScreen(
                                    onAuthenticated = dropUnlessResumed {
                                        backStack.clear()
                                        backStack.add(ConversationListRoute)
                                    },
                                )
                            }

                            is ConversationListRoute -> NavEntry(key) {
                                ConversationListScreen(
                                    onOpenConversation = dropUnlessResumed { id ->
                                        backStack.add(ChatRoute(id))
                                    },
                                    onOpenInvitations = dropUnlessResumed {
                                        backStack.add(InvitationsRoute)
                                    },
                                )
                            }

                            is ChatRoute -> NavEntry(key) {
                                ChatScreen(
                                    conversationId = key.conversationId,
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                )
                            }

                            is InvitationsRoute -> NavEntry(key) {
                                InvitationsScreen(
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                )
                            }

                            else -> error("Unknown route: $key")
                        }
                    },
                )
            }
        }
    }
}
