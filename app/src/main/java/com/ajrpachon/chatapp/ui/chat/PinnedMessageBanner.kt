package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.MessageBO

// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — self-contained, no dependency
// on ChatViewModel or the message-bubble rendering tree.

/**
 * [onHide] collapses the banner locally for the current [message] without unpinning it (it
 * comes back if the pinned message changes, or the screen is reopened — same
 * `rememberSaveable(latestPinned?.id)` key `ChatScreen` already keyed the old dead
 * `pinnedBannerVisible` field on). Distinct from [onDismiss], which actually unpins the message
 * server-side via `ChatIntent.UnpinMessage`.
 */
@Composable
internal fun PinnedMessageBanner(
    message: MessageBO,
    pinnedCount: Int,
    onTap: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pinnedCount > 1) stringResource(R.string.chat_pinned_message_count, pinnedCount) else stringResource(R.string.chat_pinned_message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = message.content.ifBlank { stringResource(R.string.chat_attachment_placeholder) },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            IconButton(onClick = onHide) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.chat_hide_pinned_banner), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_unpin), modifier = Modifier.size(16.dp))
            }
        }
    }
}
