package com.ajrpachon.chatapp.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.ajrpachon.chatapp.ui.preview.ChatAppPreview
import com.ajrpachon.chatapp.ui.preview.ThemePreviews
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A circular filled icon button used throughout the call UI (in-call controls, incoming-call
 * accept/reject). Shared between [CallScreen] and [IncomingCallScreen] — was previously
 * duplicated as hand-rolled [FilledIconButton] blocks in the latter.
 */
@Composable
fun CallControlButton(
    onClick: () -> Unit,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    content: @Composable () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = iconTint,
        ),
    ) { content() }
}

@ThemePreviews
@Composable
internal fun CallControlButtonPreview() {
    ChatAppPreview {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CallControlButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            ) { Icon(Icons.Default.Mic, contentDescription = null) }
            CallControlButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            ) { Icon(Icons.Default.MicOff, contentDescription = null) }
            CallControlButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.primary,
                iconTint = MaterialTheme.colorScheme.onPrimary,
                size = 72.dp,
            ) { Icon(Icons.Default.Call, contentDescription = null) }
            CallControlButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.onError,
                size = 72.dp,
            ) { Icon(Icons.Default.CallEnd, contentDescription = null) }
        }
    }
}
