package com.ajrpachon.chatapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ajrpachon.chatapp.utils.RootDetector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.utils.AppLogger
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.ajrpachon.chatapp.ui.auth.AuthScreen
import com.ajrpachon.chatapp.ui.auth.IntegrityBlockedScreen
import com.ajrpachon.chatapp.utils.IntegrityChecker
import com.ajrpachon.chatapp.utils.IntegrityResult
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
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
import com.ajrpachon.chatapp.utils.SessionGuard
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import com.github.skydoves.navgraph.annotations.NavGraphRoot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    // Signals that the session has expired and the UI should redirect to AuthRoute
    private val sessionExpired = mutableStateOf(false)
    private var showRootWarning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        requestNotificationPermissionIfNeeded()
        checkRootAndWarnIfNeeded()
        pendingConversationId.value = intent.validatedConversationId()
        pendingOtherUserName.value = intent.validatedUserName()
        val getCurrentUser: GetCurrentUserUseCase = get()
        val supabase: SupabaseClient = get()
        setContent {
            ChatAppTheme {
                // ── 1. Play Integrity gate ──────────────────────────────────
                val integrityResult by produceState<IntegrityResult?>(initialValue = null) {
                    value = IntegrityChecker.check(this@MainActivity, supabase)
                }

                when (val integrity = integrityResult) {
                    null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                        return@ChatAppTheme
                    }
                    is IntegrityResult.Failed -> {
                        IntegrityBlockedScreen(onExit = { finish() })
                        return@ChatAppTheme
                    }
                    is IntegrityResult.Error -> {
                        AppLogger.w("MainActivity", "Integrity check error (allowing): ${integrity.message}")
                    }
                    is IntegrityResult.Passed -> Unit
                }

                // ── 2. Normal app flow ──────────────────────────────────────
                val sessionGuard: SessionGuard = get()
                val isExpired by sessionExpired
                val initialRoute by produceState<NavKey?>(initialValue = null) {
                    val hasUser = getCurrentUser().first() != null
                    value = when {
                        !hasUser -> AuthRoute
                        sessionGuard.isSessionExpired() -> {
                            // Sign out server-side and clear local guard before routing
                            CoroutineScope(Dispatchers.IO).launch {
                                runCatching { get<SupabaseClient>().auth.signOut() }
                                sessionGuard.clearSession()
                            }
                            AuthRoute
                        }
                        else -> ConversationListRoute
                    }
                }
                val resolvedRoute = initialRoute ?: return@ChatAppTheme
                val backStack = rememberNavBackStack(resolvedRoute)

                // Handle mid-session expiry detected in onResume
                androidx.compose.runtime.LaunchedEffect(isExpired) {
                    if (isExpired) {
                        sessionExpired.value = false
                        backStack.clear()
                        backStack.add(AuthRoute)
                    }
                }

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
                    AppLogger.d("MainActivity", "RECOMPOSE vmHash=${System.identityHashCode(incomingCallVm)} incomingCall=${incomingCallState.incomingCall?.id ?: "null"}")
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

                // Root warning dialog — shown only once on first launch if root is detected
                if (showRootWarning) {
                    AlertDialog(
                        onDismissRequest = { /* non-dismissable via back/outside tap */ },
                        title = { Text("Rooted device detected") },
                        text = {
                            Text(
                                "Running on a rooted device may compromise the security of your messages. " +
                                "Do you want to continue?"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                saveRootWarningAccepted()
                                showRootWarning = false
                            }) {
                                Text("Continue")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) {
                                Text("Exit")
                            }
                        },
                    )
                }
                } // end Box
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sessionGuard: SessionGuard = get()
        if (sessionGuard.isSessionExpired()) {
            // Mid-session expiry: sign out and signal the UI to navigate to AuthRoute
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { get<SupabaseClient>().auth.signOut() }
                sessionGuard.clearSession()
            }
            sessionExpired.value = true
        } else {
            sessionGuard.recordActivity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data
        if (uri != null && uri.isValidAuthCallback()) {
            get<SupabaseClient>().handleDeeplinks(intent)
        } else if (uri != null) {
            AppLogger.w("MainActivity", "Rejected deep link with unexpected scheme/host: $uri")
        }
        intent.validatedConversationId()?.let {
            pendingConversationId.value = it
            pendingOtherUserName.value = intent.validatedUserName()
        }
    }

    private fun Uri.isValidAuthCallback(): Boolean =
        scheme == "com.ajrpachon.chatapp" && host == "auth-callback"

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    private fun checkRootAndWarnIfNeeded() {
        val prefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        val alreadyAccepted = prefs.getBoolean("root_warning_accepted", false)
        if (!alreadyAccepted && RootDetector.isRooted(packageManager)) {
            showRootWarning = true
        }
    }

    private fun saveRootWarningAccepted() {
        getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("root_warning_accepted", true)
            .apply()
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
