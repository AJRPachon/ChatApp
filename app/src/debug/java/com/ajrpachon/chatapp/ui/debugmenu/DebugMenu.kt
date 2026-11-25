package com.ajrpachon.chatapp.ui.debugmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar

/**
 * QA / Debug menu — debug builds only.
 *
 * Gives manual testers and the Maestro E2E suite a way into screens that have no entry point in
 * the regular UI yet (broadcast lists, usage stats). The release and benchmark source sets ship a
 * no-op object with this same FQN, so none of this reaches production builds.
 */
object DebugMenu {

    const val IS_ENABLED: Boolean = true

    /** Row shown at the bottom of the Profile screen. */
    @Composable
    fun ProfileEntry(onClick: () -> Unit) {
        Spacer(Modifier.size(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_debug_menu_row"),
        ) {
            DebugRow(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.debug_menu_title),
                subtitle = stringResource(R.string.debug_menu_subtitle),
                onClick = onClick,
            )
        }
    }

    @Composable
    fun Screen(
        onBack: () -> Unit,
        onOpenBroadcastLists: () -> Unit,
        onOpenUsageStats: () -> Unit,
    ) {
        Scaffold(
            topBar = { ChatAppTopBar(title = stringResource(R.string.debug_menu_title), onBack = onBack) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .testTag("debug_menu_screen"),
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    DebugRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = stringResource(R.string.debug_menu_broadcast_lists),
                        subtitle = stringResource(R.string.debug_menu_broadcast_lists_subtitle),
                        onClick = onOpenBroadcastLists,
                        modifier = Modifier.testTag("debug_open_broadcast"),
                    )
                    HorizontalDivider()
                    DebugRow(
                        icon = Icons.Default.BarChart,
                        title = stringResource(R.string.debug_menu_usage_stats),
                        subtitle = stringResource(R.string.debug_menu_usage_stats_subtitle),
                        onClick = onOpenUsageStats,
                        modifier = Modifier.testTag("debug_open_usage_stats"),
                    )
                }
            }
        }
    }

    @Composable
    private fun DebugRow(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
