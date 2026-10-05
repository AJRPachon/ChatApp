package com.ajrpachon.chatapp.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ajrpachon.chatapp.domain.model.ChatTheme
import com.ajrpachon.chatapp.ui.theme.ChatThemeDefaultBubble
import com.ajrpachon.chatapp.ui.theme.ChatThemeForestBackground
import com.ajrpachon.chatapp.ui.theme.ChatThemeForestBubble
import com.ajrpachon.chatapp.ui.theme.ChatThemeLavenderBackground
import com.ajrpachon.chatapp.ui.theme.ChatThemeLavenderBubble
import com.ajrpachon.chatapp.ui.theme.ChatThemeMidnightBackground
import com.ajrpachon.chatapp.ui.theme.ChatThemeMidnightBubble
import com.ajrpachon.chatapp.ui.theme.ChatThemeOceanBackground
import com.ajrpachon.chatapp.ui.theme.ChatThemeOceanBubble
import com.ajrpachon.chatapp.ui.theme.ChatThemeRoseBackground
import com.ajrpachon.chatapp.ui.theme.ChatThemeRoseBubble
import com.ajrpachon.chatapp.ui.theme.ChatThemeSunsetBackground
import com.ajrpachon.chatapp.ui.theme.ChatThemeSunsetBubble

data class ChatThemeColors(val bubbleColor: Color, val backgroundTint: Color)

fun ChatTheme.toColors(): ChatThemeColors = when (this) {
    ChatTheme.DEFAULT -> ChatThemeColors(ChatThemeDefaultBubble, Color.Transparent)
    ChatTheme.OCEAN -> ChatThemeColors(ChatThemeOceanBubble, ChatThemeOceanBackground)
    ChatTheme.SUNSET -> ChatThemeColors(ChatThemeSunsetBubble, ChatThemeSunsetBackground)
    ChatTheme.FOREST -> ChatThemeColors(ChatThemeForestBubble, ChatThemeForestBackground)
    ChatTheme.LAVENDER -> ChatThemeColors(ChatThemeLavenderBubble, ChatThemeLavenderBackground)
    ChatTheme.ROSE -> ChatThemeColors(ChatThemeRoseBubble, ChatThemeRoseBackground)
    ChatTheme.MIDNIGHT -> ChatThemeColors(ChatThemeMidnightBubble, ChatThemeMidnightBackground)
}

/**
 * A message bubble's content (text, reply/status quotes) normally reads its color from
 * `MaterialTheme.colorScheme` (onSurface, onSurfaceVariant, primary...), which only contrasts
 * correctly when the bubble itself is actually painted with a real color-scheme token
 * (`primaryContainer`, `surfaceVariant`). A chosen [ChatTheme]'s `bubbleColor` above is a fixed
 * pastel/dark RGB picked independently of the current light/dark scheme — Compose's own
 * `Surface`'s automatic `contentColorFor()` can't find a matching "on" color for an arbitrary
 * custom Color like that, so it silently falls back to the ambient (unrelated) content color
 * instead. In dark mode that ambient fallback is light text, landing on a light pastel bubble
 * (e.g. LAVENDER) — nearly invisible. This computes an explicit, always-correct content color
 * from the bubble's own luminance so sent-message bubbles stay legible regardless of the chosen
 * chat theme or the current system theme.
 */
fun Color.bubbleContentColor(): Color =
    if (luminance() > 0.5f) Color.Black.copy(alpha = 0.87f) else Color.White
