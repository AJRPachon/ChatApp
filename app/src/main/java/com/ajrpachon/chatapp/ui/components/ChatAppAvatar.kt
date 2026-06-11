package com.ajrpachon.chatapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.ui.theme.AvatarColors
import kotlin.math.abs

/**
 * Circular avatar used throughout the app for users and groups.
 * Shows [url] when available, otherwise a letter/icon placeholder.
 */
@Composable
fun ChatAppAvatar(
    name: String,
    modifier: Modifier = Modifier,
    url: String? = null,
    isGroup: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = size * 0.5f,
) {
    val resolvedModifier = modifier
        .size(size)
        .clip(CircleShape)

    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = resolvedModifier,
        )
    } else {
        val bgColor = AvatarColors[abs(name.hashCode()) % AvatarColors.size]
        Box(
            modifier = resolvedModifier.background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                )
            }
        }
    }
}
