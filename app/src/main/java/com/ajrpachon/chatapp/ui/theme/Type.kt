package com.ajrpachon.chatapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val base = Typography()

/**
 * "Señal" type scale: monospace for structural / metadata text — screen titles, headlines,
 * timestamps, badges — so it reads as data rather than decoration. Anything read at length
 * (contact names, message bodies, button labels) stays on the platform sans for legibility.
 *
 * Built as a `copy()` of the M3 default so every other slot (sizes, line heights, letter
 * spacing) keeps its platform-correct value — only `fontFamily`/`fontWeight` are overridden.
 */
val ChatAppTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
)
