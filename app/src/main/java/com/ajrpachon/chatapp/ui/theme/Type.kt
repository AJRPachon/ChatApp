package com.ajrpachon.chatapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/**
 * "Señal" type scale: monospace for structural / metadata text — screen titles, headlines,
 * timestamps, badges — so it reads as data rather than decoration. Anything read at length
 * (contact names, message bodies, button labels) stays on the platform sans for legibility.
 *
 * Built as a `copy()` of the M3 default so every other slot (sizes, line heights, letter
 * spacing) keeps its platform-correct value — only `fontFamily`/`fontWeight` are overridden.
 */
@Suppress("TopLevelPropertyNaming") // PascalCase like every Compose theme object (Typography, Shapes)
val ChatAppTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
)

/**
 * Text sizes for content that is not a Material text style: emoji and sticker glyphs and one
 * emphasised section label. Kept here so a screen never spells a size itself.
 */
object ChatAppTextSizes {
    /** An emoji drawn as a category icon in the emoji picker tabs. */
    val EmojiCategory = 18.sp

    /** An emoji in the picker grid. */
    val EmojiCell = 24.sp

    /** The "+" glyph on the sticker store tab. */
    val TabAction = 20.sp

    /** An emoji sticker shown as a message. */
    val StickerMessage = 64.sp

    /** Section label that has to stand out in the profile screen. */
    val SectionLabel = 13.sp
}
