package com.ajrpachon.chatapp

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ajrpachon.chatapp.ui.applock.AppLockScreen
import com.ajrpachon.chatapp.ui.auth.AuthScreen
import com.ajrpachon.chatapp.ui.backup.BackupScreen
import com.ajrpachon.chatapp.ui.broadcast.BroadcastListScreen
import com.ajrpachon.chatapp.ui.call.CallScreen
import com.ajrpachon.chatapp.ui.chat.ChatMediaGalleryScreen
import com.ajrpachon.chatapp.ui.chat.ChatScreen
import com.ajrpachon.chatapp.ui.conversations.ConversationListScreen
import com.ajrpachon.chatapp.ui.group.CreateGroupScreen
import com.ajrpachon.chatapp.ui.group.GroupInfoScreen
import com.ajrpachon.chatapp.ui.invitations.InvitationsScreen
import com.ajrpachon.chatapp.ui.newchat.NewChatScreen
import com.ajrpachon.chatapp.ui.pdf.PdfViewerScreen
import com.ajrpachon.chatapp.ui.profile.ProfileScreen
import com.ajrpachon.chatapp.ui.profile.SessionAuditScreen
import com.ajrpachon.chatapp.ui.search.GlobalSearchScreen
import com.ajrpachon.chatapp.ui.usagestats.UsageStatsScreen
import com.ajrpachon.chatapp.ui.userinfo.UserInfoScreen
import com.github.skydoves.navgraph.annotations.NavGraphRoot
import kotlinx.serialization.Serializable

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
@Serializable data object BroadcastListRoute : NavKey
@Serializable data object UsageStatsRoute : NavKey
@Serializable data object SessionAuditRoute : NavKey
@Serializable data object AppLockRoute : NavKey
@Serializable data object BackupRoute : NavKey
@Serializable data class PdfViewerRoute(val url: String, val filename: String) : NavKey
@Serializable data object GlobalSearchRoute : NavKey
@Serializable data class ChatMediaGalleryRoute(val conversationId: String, val conversationName: String) : NavKey

// ── NavEntry providers ─────────────────────────────────────────────────────

/** NavEntry providers grouped by feature area. Each returns a [NavEntry] or null. */

fun mainNavEntry(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<*>? = when (key) {
    is AuthRoute -> NavEntry(key) {
        AuthScreen(
            onAuthenticated = dropUnlessResumed {
                backStack.clear()
                backStack.add(ConversationListRoute)
            },
        )
    }

    is AppLockRoute -> NavEntry(key) {
        AppLockScreen(
            onUnlocked = dropUnlessResumed {
                backStack.removeAll { it is AppLockRoute }
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
            onGoToGlobalSearch = dropUnlessResumed {
                backStack.add(GlobalSearchRoute)
            },
        )
    }

    else -> null
}

fun chatNavEntry(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<*>? = when (key) {
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
            onOpenPdf = { url, filename ->
                backStack.add(PdfViewerRoute(url, filename))
            },
            onOpenMediaGallery = dropUnlessResumed {
                backStack.add(
                    ChatMediaGalleryRoute(
                        conversationId = key.conversationId,
                        conversationName = key.otherUserName,
                    )
                )
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

    else -> null
}

fun callNavEntry(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<*>? = when (key) {
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

    else -> null
}

fun groupNavEntry(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<*>? = when (key) {
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

    else -> null
}

fun profileNavEntry(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<*>? = when (key) {
    is ProfileRoute -> NavEntry(key) {
        ProfileScreen(
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
            onSignOut = {
                backStack.clear()
                backStack.add(AuthRoute)
            },
            onBackup = dropUnlessResumed {
                backStack.add(BackupRoute)
            },
            onSessionAudit = dropUnlessResumed {
                backStack.add(SessionAuditRoute)
            },
        )
    }

    is BackupRoute -> NavEntry(key) {
        BackupScreen(
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    is SessionAuditRoute -> NavEntry(key) {
        SessionAuditScreen(
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    is UsageStatsRoute -> NavEntry(key) {
        UsageStatsScreen(
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    else -> null
}

fun miscNavEntry(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<*>? = when (key) {
    is UserInfoRoute -> NavEntry(key) {
        UserInfoScreen(
            userId = key.userId,
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    is BroadcastListRoute -> NavEntry(key) {
        BroadcastListScreen(
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    is PdfViewerRoute -> NavEntry(key) {
        PdfViewerScreen(
            url = key.url,
            filename = key.filename,
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    is GlobalSearchRoute -> NavEntry(key) {
        GlobalSearchScreen(
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
            onOpenConversation = { id, name, isGroup ->
                backStack.removeAll { it is GlobalSearchRoute }
                backStack.add(ChatRoute(id, name, isGroup))
            },
        )
    }

    is ChatMediaGalleryRoute -> NavEntry(key) {
        ChatMediaGalleryScreen(
            conversationId = key.conversationId,
            conversationName = key.conversationName,
            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
        )
    }

    else -> null
}

/** Single entry point that delegates to the feature-specific providers. */
@Suppress("UNCHECKED_CAST")
fun appNavEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey> = (
    mainNavEntry(key, backStack)
        ?: chatNavEntry(key, backStack)
        ?: callNavEntry(key, backStack)
        ?: groupNavEntry(key, backStack)
        ?: profileNavEntry(key, backStack)
        ?: miscNavEntry(key, backStack)
        ?: error("Unknown route: $key")
) as NavEntry<NavKey>
