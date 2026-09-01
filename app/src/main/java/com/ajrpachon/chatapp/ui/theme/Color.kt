package com.ajrpachon.chatapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── "Señal" — graphite / amber, disciplina de terminal ─────────────────────
// Base casi monocroma (grafito con sesgo verde-azulado) con un único acento
// ámbar. Contenedores y outline se apoyan en la misma familia de grises para
// que el ámbar sea lo único que llame la atención.

// ── Light scheme ─────────────────────────────────────────────────────────
val Signal_Primary = Color(0xFF8A6423)
val Signal_OnPrimary = Color(0xFFFFFFFF)
val Signal_PrimaryContainer = Color(0xFFF6E1B8)
val Signal_OnPrimaryContainer = Color(0xFF2B1D02)

val Signal_Secondary = Color(0xFF3F6358)
val Signal_OnSecondary = Color(0xFFFFFFFF)
val Signal_SecondaryContainer = Color(0xFFD2E9DF)
val Signal_OnSecondaryContainer = Color(0xFF06201A)

val Signal_Tertiary = Color(0xFF1F6B48)
val Signal_OnTertiary = Color(0xFFFFFFFF)
val Signal_TertiaryContainer = Color(0xFFB4EFCE)
val Signal_OnTertiaryContainer = Color(0xFF002111)

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
val Signal_InversePrimary = Color(0xFFE4B975)

// ── Dark scheme ─────────────────────────────────────────────────────────
val Signal_PrimaryDark = Color(0xFFD9A455)
val Signal_OnPrimaryDark = Color(0xFF191106)
val Signal_PrimaryContainerDark = Color(0xFF4C3814)
val Signal_OnPrimaryContainerDark = Color(0xFFF2D9AE)

val Signal_SecondaryDark = Color(0xFF96C4B8)
val Signal_OnSecondaryDark = Color(0xFF0A2820)
val Signal_SecondaryContainerDark = Color(0xFF234A40)
val Signal_OnSecondaryContainerDark = Color(0xFFB9E2D6)

val Signal_TertiaryDark = Color(0xFF7FE0B3)
val Signal_OnTertiaryDark = Color(0xFF06291B)
val Signal_TertiaryContainerDark = Color(0xFF16402C)
val Signal_OnTertiaryContainerDark = Color(0xFFB7EDD3)

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
val Signal_InversePrimaryDark = Color(0xFF8A6423)

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
