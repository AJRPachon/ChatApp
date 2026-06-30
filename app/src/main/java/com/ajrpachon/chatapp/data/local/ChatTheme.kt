package com.ajrpachon.chatapp.data.local

import androidx.compose.ui.graphics.Color

enum class ChatTheme(
    val bubbleColor: Color,
    val backgroundTint: Color,
) {
    DEFAULT(
        bubbleColor = Color(0xFFDCE8FB),
        backgroundTint = Color.Transparent,
    ),
    OCEAN(
        bubbleColor = Color(0xFF9CD3E8),
        backgroundTint = Color(0xFFE8F5FA),
    ),
    SUNSET(
        bubbleColor = Color(0xFFF4B8A0),
        backgroundTint = Color(0xFFFDF0EB),
    ),
    FOREST(
        bubbleColor = Color(0xFFA8D5B5),
        backgroundTint = Color(0xFFEBF5EE),
    ),
    LAVENDER(
        bubbleColor = Color(0xFFCDB8E8),
        backgroundTint = Color(0xFFF3EEF9),
    ),
    ROSE(
        bubbleColor = Color(0xFFF2A8C0),
        backgroundTint = Color(0xFFFDF0F4),
    ),
    MIDNIGHT(
        bubbleColor = Color(0xFF5D6A8A),
        backgroundTint = Color(0xFF1A1F2E),
    ),
}
