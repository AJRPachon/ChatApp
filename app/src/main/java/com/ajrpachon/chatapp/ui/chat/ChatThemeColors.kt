package com.ajrpachon.chatapp.ui.chat

import androidx.compose.ui.graphics.Color
import com.ajrpachon.chatapp.domain.model.ChatTheme

data class ChatThemeColors(val bubbleColor: Color, val backgroundTint: Color)

fun ChatTheme.toColors(): ChatThemeColors = when (this) {
    ChatTheme.DEFAULT -> ChatThemeColors(Color(0xFFDCE8FB), Color.Transparent)
    ChatTheme.OCEAN -> ChatThemeColors(Color(0xFF9CD3E8), Color(0xFFE8F5FA))
    ChatTheme.SUNSET -> ChatThemeColors(Color(0xFFF4B8A0), Color(0xFFFDF0EB))
    ChatTheme.FOREST -> ChatThemeColors(Color(0xFFA8D5B5), Color(0xFFEBF5EE))
    ChatTheme.LAVENDER -> ChatThemeColors(Color(0xFFCDB8E8), Color(0xFFF3EEF9))
    ChatTheme.ROSE -> ChatThemeColors(Color(0xFFF2A8C0), Color(0xFFFDF0F4))
    ChatTheme.MIDNIGHT -> ChatThemeColors(Color(0xFF5D6A8A), Color(0xFF1A1F2E))
}
