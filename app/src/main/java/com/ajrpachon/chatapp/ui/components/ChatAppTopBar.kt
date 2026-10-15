package com.ajrpachon.chatapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.MoreVert
import com.ajrpachon.chatapp.ui.preview.ChatAppPreview
import com.ajrpachon.chatapp.ui.preview.ThemePreviews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import com.ajrpachon.chatapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = dropUnlessResumed { onBack() },
                modifier = Modifier.testTag("chat_app_top_bar_back_button"),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.chat_back))
            }
        },
        actions = actions,
        modifier = modifier,
    )
}

@ThemePreviews
@Composable
internal fun ChatAppTopBarPreview() {
    ChatAppPreview {
        Column {
            ChatAppTopBar(title = "Ajustes", onBack = {})
            ChatAppTopBar(
                title = "Un título de pantalla muy largo que no cabe en una sola línea",
                onBack = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                },
            )
        }
    }
}
