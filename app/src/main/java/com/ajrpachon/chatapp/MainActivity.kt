package com.ajrpachon.chatapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.ajrpachon.chatapp.utils.AppLogger
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.ajrpachon.chatapp.ui.auth.IntegrityBlockedScreen
import com.ajrpachon.chatapp.domain.repository.AppLockRepository
import com.ajrpachon.chatapp.utils.IntegrityChecker
import com.ajrpachon.chatapp.utils.IntegrityResult
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import com.ajrpachon.chatapp.ui.call.IncomingCallIntent
import com.ajrpachon.chatapp.ui.call.IncomingCallScreen
import com.ajrpachon.chatapp.ui.call.IncomingCallViewModel
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import com.ajrpachon.chatapp.data.local.ThemePreference
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.utils.SessionGuard
import kotlinx.coroutines.flow.first

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.compose.runtime.produceState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.compose.koinViewModel

// ── Activity ───────────────────────────────────────────────────────────────

private const val APP_LOCK_TIMEOUT_MS = 30_000L

class MainActivity : ComponentActivity() {

    private val pendingConversationId = mutableStateOf<String?>(null)
    private val pendingOtherUserName = mutableStateOf<String?>(null)
    // Signals that the session has expired and the UI should redirect to AuthRoute
    private val sessionExpired = mutableStateOf(false)
    private var showRootWarning by mutableStateOf(false)
    private val shouldShowAppLock = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        checkRootAndWarnIfNeeded()
        // Handle cold-start deep link (chatapp://chat/{id}) or FCM intent extras
        val coldUri = intent.data
        if (coldUri != null && coldUri.scheme == "chatapp" && coldUri.host == "chat") {
            pendingConversationId.value = coldUri.lastPathSegment?.takeIf { UUID_REGEX.matches(it) }
            pendingOtherUserName.value = coldUri.getQueryParameter("name")?.take(100)?.ifBlank { null }
        } else {
            pendingConversationId.value = intent.validatedConversationId()
            pendingOtherUserName.value = intent.validatedUserName()
        }
        val getCurrentUser: GetCurrentUserUseCase = get()
        val supabase: SupabaseClient = get()
        setContent {
            val themeRepository: ThemeRepository = get()
            val themePreference by themeRepository.observe().collectAsState(initial = ThemePreference.SYSTEM)
            val darkTheme = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
            ChatAppTheme(darkTheme = darkTheme) {
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
                            lifecycleScope.launch(Dispatchers.IO) {
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

                // Handle app lock trigger from onResume
                val showLock by shouldShowAppLock
                androidx.compose.runtime.LaunchedEffect(showLock) {
                    if (showLock) {
                        shouldShowAppLock.value = false
                        if (backStack.none { it is AppLockRoute }) {
                            backStack.add(AppLockRoute)
                        }
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
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() else finish() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = { key -> appNavEntryProvider(key, backStack) },
                )

                incomingCallState.incomingCall?.let { call ->
                    IncomingCallScreen(
                        call = call,
                        onAccept = {
                            incomingCallVm.onIntent(IncomingCallIntent.Accept(call.id))
                            backStack.add(
                                CallRoute(
                                    callId = call.id,
                                    conversationId = call.conversationId,
                                    roomName = call.roomName,
                                    callType = call.type.name.lowercase(),
                                    otherUserName = call.callerName,
                                    isOutgoing = false,
                                    isGroup = call.roomName.startsWith("group_"),
                                )
                            )
                        },
                        onReject = { incomingCallVm.onIntent(IncomingCallIntent.Reject(call.id)) },
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

    override fun onPause() {
        super.onPause()
        val appLockRepository: AppLockRepository = get()
        lifecycleScope.launch(Dispatchers.IO) {
            appLockRepository.recordBackgroundedAt(System.currentTimeMillis())
        }
    }

    override fun onResume() {
        super.onResume()
        val sessionGuard: SessionGuard = get()
        if (sessionGuard.isSessionExpired()) {
            // Mid-session expiry: sign out and signal the UI to navigate to AuthRoute
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { get<SupabaseClient>().auth.signOut() }
                sessionGuard.clearSession()
            }
            sessionExpired.value = true
        } else {
            sessionGuard.recordActivity()
        }
        // App lock check: show lock screen if enabled and backgrounded for >30s
        val appLockRepository: AppLockRepository = get()
        lifecycleScope.launch(Dispatchers.IO) {
            val isEnabled = appLockRepository.isEnabled.first()
            if (isEnabled) {
                val backgroundedAt = appLockRepository.backgroundedAt.first()
                val elapsed = System.currentTimeMillis() - backgroundedAt
                if (backgroundedAt > 0L && elapsed > APP_LOCK_TIMEOUT_MS) {
                    shouldShowAppLock.value = true
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data
        when {
            uri != null && uri.isValidAuthCallback() -> get<SupabaseClient>().handleDeeplinks(intent)
            uri != null && uri.isChatDeepLink() -> {
                val conversationId = uri.lastPathSegment?.takeIf { UUID_REGEX.matches(it) }
                val name = uri.getQueryParameter("name")?.take(100)?.ifBlank { null }
                conversationId?.let {
                    pendingConversationId.value = it
                    pendingOtherUserName.value = name
                }
            }
            uri != null -> AppLogger.w("MainActivity", "Rejected deep link with unexpected scheme/host: $uri")
        }
        // Fallback: Intent extra from FCM notification tap
        if (uri == null) {
            intent.validatedConversationId()?.let {
                pendingConversationId.value = it
                pendingOtherUserName.value = intent.validatedUserName()
            }
        }
    }

    private fun Uri.isValidAuthCallback(): Boolean =
        scheme == "com.ajrpachon.chatapp" && host == "auth-callback"

    private fun Uri.isChatDeepLink(): Boolean =
        scheme == "chatapp" && host == "chat"

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
