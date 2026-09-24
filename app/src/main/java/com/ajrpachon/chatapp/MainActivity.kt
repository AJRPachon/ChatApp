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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import com.ajrpachon.chatapp.ui.call.IncomingCallIntent
import com.ajrpachon.chatapp.domain.model.isGroupCall
import com.ajrpachon.chatapp.ui.call.IncomingCallScreen
import com.ajrpachon.chatapp.ui.call.IncomingCallViewModel
import com.ajrpachon.chatapp.ui.common.AppSplashScreen
import com.ajrpachon.chatapp.ui.common.MotionConstants.NAV_TRANSITION_MS
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import com.ajrpachon.chatapp.domain.model.ThemePreference
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.utils.SessionGuard
import kotlinx.coroutines.flow.first

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.utils.AnalyticsEvents
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

    // Read from setKeepOnScreenCondition's lambda, which the platform polls on its own thread
    // outside Compose — set true from a Compose SideEffect on the very first frame Compose draws
    // (see setContent below), NOT once integrity/route resolve. Android always paints something
    // of its own before any app process can draw a single frame — that gap can't be skipped — so
    // the system's SplashScreen (styled as a plain "Opción I" icon on the right background, see
    // themes.xml) only needs to cover THAT gap. The moment our first Compose frame is ready it
    // takes over, and that first frame is AppSplashScreen (the full "Splash · oscuro y claro"
    // design: icon + wordmark + tagline) for as long as the integrity check and initial route
    // are still resolving — one continuous handoff, system icon straight into the real splash,
    // with nothing blank or unbranded in between.
    @Volatile
    private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !contentReady }
        // The default exit transition fades the system's own icon out (~200ms) while our
        // AppSplashScreen — which reproduces that same icon — is already visible underneath.
        // Since both icons are similar, that fade reads as the icon briefly going dim/muddy
        // instead of a clean handoff. Skip the animation: remove the system splash the instant
        // it's allowed to go, so only our (correct, full-color) icon is ever on screen.
        splashScreen.setOnExitAnimationListener { it.remove() }
        enableEdgeToEdge()
        // enableEdgeToEdge() sets window.isNavigationBarContrastEnforced = true on API 29-34,
        // which paints a translucent system scrim over the 3-button navigation bar. Screens with
        // their own bottom bar (e.g. ChatScreen's ChatBottomBar) already paint a solid background
        // that extends into the navigationBars inset, so that scrim only dims/shifts that color
        // and can read as a seam right at the nav bar — disable it so our own bottom bar color is
        // what's actually shown, uncontested, all the way to the screen edge.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
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
        if (pendingConversationId.value != null) logNotificationOpened()
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
                // This frame is drawing, so the system SplashScreen can come down now — whether
                // what follows below is AppSplashScreen or the real content depends on the gate
                // just after, but either way something of ours is now on screen.
                SideEffect { contentReady = true }

                // ── 1. Play Integrity gate ──────────────────────────────────
                val integrityResult by produceState<IntegrityResult?>(initialValue = null) {
                    value = IntegrityChecker.check(this@MainActivity, supabase)
                }

                val sessionGuard: SessionGuard = get()
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

                if (integrityResult == null || initialRoute == null) {
                    AppSplashScreen(darkTheme = darkTheme)
                    return@ChatAppTheme
                }

                when (val integrity = integrityResult) {
                    is IntegrityResult.Failed -> {
                        IntegrityBlockedScreen(onExit = { finish() })
                        return@ChatAppTheme
                    }
                    is IntegrityResult.Error -> {
                        AppLogger.w("MainActivity", "Integrity check error (allowing): ${integrity.message}")
                    }
                    is IntegrityResult.Passed, null -> Unit
                }

                // ── 2. Normal app flow ──────────────────────────────────────
                val isExpired by sessionExpired
                val resolvedRoute = initialRoute ?: return@ChatAppTheme
                val backStack = rememberNavBackStack(resolvedRoute)

                // Screen-view tracking: single chokepoint for every navigation change, no
                // per-screen wiring needed. Route class name only — routes carry ids/names
                // (ChatRoute, UserInfoRoute...) that must never reach analytics.
                val analyticsTracker = remember { get<AnalyticsTracker>() }
                LaunchedEffect(backStack.lastOrNull()) {
                    val screenName = backStack.lastOrNull()?.let { it::class.simpleName } ?: return@LaunchedEffect
                    analyticsTracker.logEvent(
                        AnalyticsEvents.SCREEN_VIEW,
                        mapOf(
                            AnalyticsEvents.PARAM_SCREEN_NAME to screenName,
                            AnalyticsEvents.PARAM_SCREEN_CLASS to screenName,
                        ),
                    )
                }

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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Exposes Compose testTag() as the Android resource-id so UI
                        // automation tools (Maestro, UiAutomator) can select on it.
                        .semantics { testTagsAsResourceId = true },
                ) {
                NavDisplay(
                    backStack = backStack,
                    // Security: AppLockRoute must never be dismissible by a plain back
                    // press/gesture — that would let anyone bypass the lock screen and
                    // land straight on whatever was underneath (chat list, an open
                    // conversation, etc.) with zero authentication. Route it to the
                    // same "leave the app, don't reveal what's behind the lock" behavior
                    // as pressing Home, instead of popping the lock screen off the stack.
                    onBack = {
                        when {
                            backStack.lastOrNull() is AppLockRoute -> moveTaskToBack(true)
                            // An active call must never be hung up by a plain back press/gesture
                            // — that's what the in-call hang-up button is for. Route back to the
                            // same "leave the app, keep what's running" behavior as Home instead
                            // of popping CallRoute off the stack, which would clear CallViewModel
                            // (rememberViewModelStoreNavEntryDecorator) and disconnect the room.
                            backStack.lastOrNull() is CallRoute -> moveTaskToBack(true)
                            backStack.size > 1 -> backStack.removeLastOrNull()
                            else -> finish()
                        }
                    },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    // Motion for every screen-to-screen navigation in the app — NavDisplay has no
                    // transitionSpec/popTransitionSpec of its own by default, so without this every
                    // push/pop across the whole app (opening a chat, Profile, Invitations, a call...)
                    // was an instant, un-animated cut. Material's "shared axis X": pushing slides the
                    // new screen in from the right while the one underneath slides out partway left
                    // (both cross-fading together), and popping reverses it — forward always reads as
                    // "deeper", back always reads as "returning", matching the platform's own
                    // Activity/Fragment transition convention. `it / 3` and `it / 4` (not `it`, a full
                    // off-screen slide) keep the outgoing screen partially visible throughout, which
                    // is what makes the two screens read as one continuous motion instead of two
                    // separate slides.
                    //
                    // StatusViewerRoute/CallRoute are the exception: both are full-bleed, edge-to-edge
                    // immersive overlays (a story's own colored/photo background, a call's camera feed)
                    // opened "on top of" whatever you were doing, not a new place in the app's normal
                    // screen hierarchy — sliding them in/out alongside the screen underneath like a
                    // regular page reads as the wrong kind of motion for them (WhatsApp/Instagram don't
                    // slide a story open either). They get a fade+scale "modal" pop/dismiss instead:
                    // grows in from slightly smaller when opened, shrinks back down when closed —
                    // scale target chosen conservatively (0.92f) so it reads as "settling into place"
                    // rather than a jarring zoom.
                    transitionSpec = {
                        if (targetState.key is StatusViewerRoute || targetState.key is CallRoute) {
                            (fadeIn(tween(NAV_TRANSITION_MS)) + scaleIn(tween(NAV_TRANSITION_MS), initialScale = 0.92f)) togetherWith
                                fadeOut(tween(NAV_TRANSITION_MS / 2))
                        } else {
                            (
                                slideInHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeIn(tween(NAV_TRANSITION_MS))
                                ) togetherWith (
                                slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { -it / 4 } +
                                    fadeOut(tween(NAV_TRANSITION_MS / 2))
                                )
                        }
                    },
                    popTransitionSpec = {
                        if (initialState.key is StatusViewerRoute || initialState.key is CallRoute) {
                            fadeIn(tween(NAV_TRANSITION_MS / 2)) togetherWith
                                (fadeOut(tween(NAV_TRANSITION_MS)) + scaleOut(tween(NAV_TRANSITION_MS), targetScale = 0.92f))
                        } else {
                            (
                                slideInHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { -it / 4 } +
                                    fadeIn(tween(NAV_TRANSITION_MS))
                                ) togetherWith (
                                slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeOut(tween(NAV_TRANSITION_MS / 2))
                                )
                        }
                    },
                    // Without this, an edge-swipe back gesture only plays popTransitionSpec's
                    // fixed-duration animation AFTER you lift your finger — nothing moves while you're
                    // actually dragging, which is exactly what read as "sosa"/lifeless: modern Android
                    // apps show the outgoing screen shrinking/sliding away live, tracking your finger,
                    // the whole time you're swiping (the "predictive back" preview, Android 13+). Same
                    // ContentTransform logic as popTransitionSpec above (deliberately identical, per
                    // Android's own recipe for this — https://developer.android.com/guide/navigation/
                    // navigation-3/recipes/animations) — the difference is entirely that NavDisplay
                    // drives *this* one continuously off the live gesture progress instead of firing it
                    // once on commit. Requires enableOnBackInvokedCallback (AndroidManifest.xml) to
                    // actually receive gesture-progress callbacks from the OS in the first place.
                    predictivePopTransitionSpec = {
                        if (initialState.key is StatusViewerRoute || initialState.key is CallRoute) {
                            fadeIn(tween(NAV_TRANSITION_MS / 2)) togetherWith
                                (fadeOut(tween(NAV_TRANSITION_MS)) + scaleOut(tween(NAV_TRANSITION_MS), targetScale = 0.92f))
                        } else {
                            (
                                slideInHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { -it / 4 } +
                                    fadeIn(tween(NAV_TRANSITION_MS))
                                ) togetherWith (
                                slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeOut(tween(NAV_TRANSITION_MS / 2))
                                )
                        }
                    },
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
                                    callType = call.type.wireValue,
                                    otherUserName = call.callerName,
                                    isOutgoing = false,
                                    isGroup = call.isGroupCall(),
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
                    logNotificationOpened()
                }
            }
            uri != null -> AppLogger.w("MainActivity", "Rejected deep link with unexpected scheme/host: $uri")
        }
        // Fallback: Intent extra from FCM notification tap
        if (uri == null) {
            intent.validatedConversationId()?.let {
                pendingConversationId.value = it
                pendingOtherUserName.value = intent.validatedUserName()
                logNotificationOpened()
            }
        }
    }

    // Wrapped defensively — same reason as ChatApplication's Firebase calls: must never crash
    // a screen navigation just because analytics collection is unavailable.
    private fun logNotificationOpened() {
        runCatching { get<AnalyticsTracker>().logEvent(AnalyticsEvents.NOTIFICATION_OPENED) }
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
