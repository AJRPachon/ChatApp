package com.ajrpachon.chatapp.ui.call

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
