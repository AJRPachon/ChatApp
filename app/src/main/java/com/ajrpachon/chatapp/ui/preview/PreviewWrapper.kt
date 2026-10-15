package com.ajrpachon.chatapp.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme

/**
 * Standard wrapper for previews of components that don't bring their own background: applies
 * [ChatAppTheme] (light or dark following the preview's `uiMode`) on the theme background color.
 */
@Composable
internal fun ChatAppPreview(content: @Composable () -> Unit) {
    ChatAppTheme {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
