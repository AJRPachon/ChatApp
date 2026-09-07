package com.ajrpachon.chatapp.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
