package com.ajrpachon.chatapp.ui.debugmenu

import androidx.compose.runtime.Composable

/**
 * No-op counterpart of the debug-only QA menu (see src/debug). Same FQN so `main` compiles
 * against it in every build type, but nothing is rendered and nothing is reachable in release.
 */
object DebugMenu {

    const val IS_ENABLED: Boolean = false

    @Composable
    @Suppress("UNUSED_PARAMETER")
    fun ProfileEntry(onClick: () -> Unit) = Unit

    @Composable
    @Suppress("UNUSED_PARAMETER")
    fun Screen(
        onBack: () -> Unit,
        onOpenBroadcastLists: () -> Unit,
        onOpenUsageStats: () -> Unit,
    ) = Unit
}
