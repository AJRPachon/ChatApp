package com.ajrpachon.chatapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.ajrpachon.chatapp.ui.auth.AuthScreen
import com.ajrpachon.chatapp.ui.call.CallScreen
import com.ajrpachon.chatapp.ui.call.IncomingCallScreen
import com.ajrpachon.chatapp.ui.call.IncomingCallViewModel
import com.ajrpachon.chatapp.ui.chat.ChatScreen
import com.ajrpachon.chatapp.ui.conversations.ConversationListScreen
import com.ajrpachon.chatapp.ui.group.CreateGroupScreen
import com.ajrpachon.chatapp.ui.group.GroupInfoScreen
import com.ajrpachon.chatapp.ui.invitations.InvitationsScreen
import com.ajrpachon.chatapp.ui.newchat.NewChatScreen
import com.ajrpachon.chatapp.ui.profile.ProfileScreen
import com.ajrpachon.chatapp.ui.userinfo.UserInfoScreen
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import com.github.skydoves.navgraph.annotations.NavGraphRoot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.compose.runtime.produceState
import org.koin.android.ext.android.get
import org.koin.androidx.compose.koinViewModel

// ── Routes ─────────────────────────────────────────────────────────────────

@NavGraphRoot
@Serializable data object AuthRoute : NavKey
@Serializable data object ConversationListRoute : NavKey
@Serializable data class ChatRoute(val conversationId: String, val otherUserName: String = "", val isGroup: Boolean = false) : NavKey
@Serializable data object InvitationsRoute : NavKey
@Serializable data object NewChatRoute : NavKey
@Serializable data object ProfileRoute : NavKey
@Serializable data class CallRoute(
    val callId: String,
    val conversationId: String,
    val roomName: String,
    val callType: String,
    val otherUserName: String,
    val isOutgoing: Boolean,
    val isGroup: Boolean = false,
) : NavKey
@Serializable data object CreateGroupRoute : NavKey
@Serializable data class UserInfoRoute(val userId: String) : NavKey
@Serializable data class GroupInfoRoute(
    val conversationId: String,
    val groupName: String,
    val groupAvatarUrl: String? = null,
    val groupDescription: String? = null,
) : NavKey

// ── Activity ───────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val pendingConversationId = mutableStateOf<String?>(null)
    private val pendingOtherUserName = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        requestNotificationPermissionIfNeeded()
        pendingConversationId.value = intent.validatedConversationId()
        pendingOtherUserName.value = intent.validatedUserName()
        val getCurrentUser: GetCurrentUserUseCase = get()
        setContent {
            ChatAppTheme {
                val initialRoute by produceState<NavKey?>(initialValue = null) {
                    value = if (getCurrentUser().first() != null) ConversationListRoute else AuthRoute
                }
                val resolvedRoute = initialRoute ?: return@ChatAppTheme
                val backStack = rememberNavBackStack(resolvedRoute)

                val conversationIdToOpen by pendingConversationId
                val otherUserNameToOpen by pendingOtherUserName
                androidx.compose.runtime.LaunchedEffect(conversationIdToOpen) {
                    val id = conversationIdToOpen ?: return@LaunchedEffect
                    val name = otherUserNameToOpen ?: ""
                    pendingConversationId.value = null
                    pendingOtherUserName.value = null
                    if (backStack.none { it is ConversationListRoute }) {
                        backStack.clear()
                        backStack.add(ConversationListRoute)
                    }
                    backStack.removeAll { it is ChatRoute || it is CallRoute }
                    backStack.add(ChatRoute(id, name))
                }

                val incomingCallVm: IncomingCallViewModel = koinViewModel()
                val incomingCallState by incomingCallVm.state.collectAsState()

                SideEffect {
                    Log.d("MainActivity", "RECOMPOSE vmHash=${System.identityHashCode(incomingCallVm)} incomingCall=${incomingCallState.incomingCall?.id ?: "null"}")
                }

                Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
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
                                    onOpenConversation = { id, name, isGroup ->
                                        backStack.add(ChatRoute(id, name, isGroup))
                                    },
                                    onOpenInvitations = dropUnlessResumed {
                                        backStack.add(InvitationsRoute)
                                    },
                                    onNewChat = dropUnlessResumed {
                                        backStack.add(NewChatRoute)
                                    },
                                    onNewGroup = dropUnlessResumed {
                                        backStack.add(CreateGroupRoute)
                                    },
                                    onOpenProfile = dropUnlessResumed {
                                        backStack.add(ProfileRoute)
                                    },
                                )
                            }

                            is ChatRoute -> NavEntry(key) {
                                ChatScreen(
                                    conversationId = key.conversationId,
                                    otherUserName = key.otherUserName,
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                    onStartCall = { call ->
                                        backStack.add(
                                            CallRoute(
                                                callId = call.id,
                                                conversationId = call.conversationId,
                                                roomName = call.roomName,
                                                callType = call.type.name.lowercase(),
                                                otherUserName = key.otherUserName,
                                                isOutgoing = true,
                                                isGroup = key.isGroup,
                                            )
                                        )
                                    },
                                    onGroupInfo = dropUnlessResumed {
                                        backStack.add(
                                            GroupInfoRoute(
                                                conversationId = key.conversationId,
                                                groupName = key.otherUserName,
                                            )
                                        )
                                    },
                                    onUserInfo = { userId ->
                                        backStack.add(UserInfoRoute(userId))
                                    },
                                )
                            }

                            is InvitationsRoute -> NavEntry(key) {
                                InvitationsScreen(
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                    onNavigateToChat = { id, name ->
                                        backStack.removeAll { it is InvitationsRoute }
                                        backStack.add(ChatRoute(id, name))
                                    },
                                )
                            }

                            is NewChatRoute -> NavEntry(key) {
                                NewChatScreen(
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                    onOpenConversation = { id, name ->
                                        backStack.removeLastOrNull()
                                        backStack.add(ChatRoute(id, name))
                                    },
                                    onOpenInvitations = dropUnlessResumed {
                                        backStack.removeLastOrNull()
                                        backStack.add(InvitationsRoute)
                                    },
                                )
                            }

                            is ProfileRoute -> NavEntry(key) {
                                ProfileScreen(
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                    onSignOut = {
                                        backStack.clear()
                                        backStack.add(AuthRoute)
                                    },
                                )
                            }

                            is CallRoute -> NavEntry(key) {
                                CallScreen(
                                    callId = key.callId,
                                    conversationId = key.conversationId,
                                    roomName = key.roomName,
                                    callType = key.callType,
                                    otherUserName = key.otherUserName,
                                    isOutgoing = key.isOutgoing,
                                    isGroup = key.isGroup,
                                    onCallEnded = { backStack.removeLastOrNull() },
                                )
                            }

                            is CreateGroupRoute -> NavEntry(key) {
                                CreateGroupScreen(
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                    onGroupCreated = { id, name ->
                                        backStack.removeLastOrNull()
                                        backStack.add(ChatRoute(id, name, isGroup = true))
                                    },
                                )
                            }

                            is GroupInfoRoute -> NavEntry(key) {
                                GroupInfoScreen(
                                    conversationId = key.conversationId,
                                    groupName = key.groupName,
                                    groupAvatarUrl = key.groupAvatarUrl,
                                    groupDescription = key.groupDescription,
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                )
                            }

                            is UserInfoRoute -> NavEntry(key) {
                                UserInfoScreen(
                                    userId = key.userId,
                                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                                )
                            }

                                else -> error("Unknown route: $key")
                        }
                    },
                )

                incomingCallState.incomingCall?.let { call ->
                    IncomingCallScreen(
                        call = call,
                        onAccept = {
                            incomingCallVm.dismiss()
                            backStack.add(
                                CallRoute(
                                    callId = call.id,
                                    conversationId = call.conversationId,
                                    roomName = call.roomName,
                                    callType = call.type.name.lowercase(),
                                    otherUserName = call.callerName,
                                    isOutgoing = false,
                                    isGroup = call.calleeId == null,
                                )
                            )
                        },
                        onReject = { incomingCallVm.reject(call.id) },
                    )
                }
                } // end Box
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        get<SupabaseClient>().handleDeeplinks(intent)
        intent.validatedConversationId()?.let {
            pendingConversationId.value = it
            pendingOtherUserName.value = intent.validatedUserName()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}

private val UUID_REGEX = Regex(
    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE,
)

private fun Intent.validatedConversationId(): String? =
    getStringExtra("conversation_id")?.takeIf { UUID_REGEX.matches(it) }

private fun Intent.validatedUserName(): String? =
    getStringExtra("other_user_name")?.take(100)?.ifBlank { null }
