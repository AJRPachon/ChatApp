package com.ajrpachon.chatapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── "Señal Media" — grafito / terracota + acero, disciplina de terminal ────
// Base casi monocroma (grafito con sesgo verde-azulado, sin cambios) con dos
// acentos: terracota (antes ámbar) para primary, acero azulado (antes verde)
// para secondary/tertiary. Contenedores y outline se apoyan en la misma
// familia de grises que ya tenía la app — solo cambia qué hue llama la
// atención.

// ── Light scheme ─────────────────────────────────────────────────────────
val Signal_Primary = Color(0xFFF0916D)
val Signal_OnPrimary = Color(0xFFFFFFFF)
val Signal_PrimaryContainer = Color(0xFFFBE5DA)
val Signal_OnPrimaryContainer = Color(0xFF4A2515)

val Signal_Secondary = Color(0xFF3D6EA5)
val Signal_OnSecondary = Color(0xFFFFFFFF)
val Signal_SecondaryContainer = Color(0xFFE3EAF7)
val Signal_OnSecondaryContainer = Color(0xFF22324A)

val Signal_Tertiary = Color(0xFF6C9BD1)
val Signal_OnTertiary = Color(0xFFFFFFFF)
val Signal_TertiaryContainer = Color(0xFFD2E0F5)
val Signal_OnTertiaryContainer = Color(0xFF102A43)

val Signal_Error = Color(0xFFA8402F)
val Signal_OnError = Color(0xFFFFFFFF)
val Signal_ErrorContainer = Color(0xFFFBDED6)
val Signal_OnErrorContainer = Color(0xFF3A0900)

val Signal_Surface = Color(0xFFF2F5F4)
val Signal_OnSurface = Color(0xFF101819)
val Signal_SurfaceVariant = Color(0xFFDDE5E2)
val Signal_OnSurfaceVariant = Color(0xFF414F4C)
val Signal_SurfaceContainerLowest = Color(0xFFFFFFFF)
val Signal_SurfaceContainerLow = Color(0xFFECF1EF)
val Signal_SurfaceContainer = Color(0xFFE6ECEA)
val Signal_SurfaceContainerHigh = Color(0xFFE0E7E4)
val Signal_SurfaceContainerHighest = Color(0xFFDAE2DF)

val Signal_Outline = Color(0xFF71827E)
val Signal_OutlineVariant = Color(0xFFD6E0DD)
val Signal_InverseSurface = Color(0xFF202B2C)
val Signal_InverseOnSurface = Color(0xFFEFF4F2)
val Signal_InversePrimary = Color(0xFFFFB08F)

// ── Dark scheme ─────────────────────────────────────────────────────────
val Signal_PrimaryDark = Color(0xFFFFC7A8)
val Signal_OnPrimaryDark = Color(0xFF401500)
val Signal_PrimaryContainerDark = Color(0xFF6B3420)
val Signal_OnPrimaryContainerDark = Color(0xFFFBE0D2)

val Signal_SecondaryDark = Color(0xFFA9C7E8)
val Signal_OnSecondaryDark = Color(0xFF0A2038)
val Signal_SecondaryContainerDark = Color(0xFF243452)
val Signal_OnSecondaryContainerDark = Color(0xFFDCE7FA)

val Signal_TertiaryDark = Color(0xFFA8C8F0)
val Signal_OnTertiaryDark = Color(0xFF06233F)
val Signal_TertiaryContainerDark = Color(0xFF1D2C46)
val Signal_OnTertiaryContainerDark = Color(0xFFDCE7FA)

val Signal_ErrorDark = Color(0xFFE2705F)
val Signal_OnErrorDark = Color(0xFF2A0B04)
val Signal_ErrorContainerDark = Color(0xFF4A1610)
val Signal_OnErrorContainerDark = Color(0xFFFFDAD2)

val Signal_SurfaceDark = Color(0xFF0E1516)
val Signal_OnSurfaceDark = Color(0xFFE9F2EF)
val Signal_SurfaceVariantDark = Color(0xFF3D4A47)
val Signal_OnSurfaceVariantDark = Color(0xFFA0B4B0)
val Signal_SurfaceContainerLowestDark = Color(0xFF090E0F)
val Signal_SurfaceContainerLowDark = Color(0xFF141C1E)
val Signal_SurfaceContainerDark = Color(0xFF182124)
val Signal_SurfaceContainerHighDark = Color(0xFF1F2B2F)
val Signal_SurfaceContainerHighestDark = Color(0xFF29373C)

val Signal_OutlineDark = Color(0xFF8AA09B)
val Signal_OutlineVariantDark = Color(0xFF28353A)
val Signal_InverseSurfaceDark = Color(0xFFE9F2EF)
val Signal_InverseOnSurfaceDark = Color(0xFF1B262A)
val Signal_InversePrimaryDark = Color(0xFFF0916D)

// ── Avatar placeholder colors ────────────────────────────────────────────
// Independientes del tema: identifican a la persona, no a la marca.
val AvatarColors = listOf(
    Color(0xFF7986CB), // indigo
    Color(0xFF66BB6A), // green
    Color(0xFFEF5350), // red
    Color(0xFFAB47BC), // purple
    Color(0xFFFFA726), // orange
    Color(0xFF26C6DA), // cyan
    Color(0xFFEC407A), // pink
    Color(0xFF8D6E63), // brown
)

// ── Fixed accent colors ──────────────────────────────────────────────────
// Independientes del tema: significado fijo (incógnito, llamada), no colores
// de marca — se mantienen igual en claro/oscuro por diseño.
val IncognitoBannerBackground = Color(0xFF4A148C)
val IncognitoAccent = Color(0xFF7B1FA2)
val CallAcceptedGreen = Color(0xFF2E7D32)
val CallBackground = Color(0xFF1A1A2E)
val CallScreenShareAccent = Color(0xFFFF5722)
