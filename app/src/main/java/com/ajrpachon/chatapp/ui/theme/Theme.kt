package com.ajrpachon.chatapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Signal_Primary,
    onPrimary = Signal_OnPrimary,
    primaryContainer = Signal_PrimaryContainer,
    onPrimaryContainer = Signal_OnPrimaryContainer,
    secondary = Signal_Secondary,
    onSecondary = Signal_OnSecondary,
    secondaryContainer = Signal_SecondaryContainer,
    onSecondaryContainer = Signal_OnSecondaryContainer,
    tertiary = Signal_Tertiary,
    onTertiary = Signal_OnTertiary,
    tertiaryContainer = Signal_TertiaryContainer,
    onTertiaryContainer = Signal_OnTertiaryContainer,
    error = Signal_Error,
    onError = Signal_OnError,
    errorContainer = Signal_ErrorContainer,
    onErrorContainer = Signal_OnErrorContainer,
    background = Signal_Surface,
    onBackground = Signal_OnSurface,
    surface = Signal_Surface,
    onSurface = Signal_OnSurface,
    surfaceVariant = Signal_SurfaceVariant,
    onSurfaceVariant = Signal_OnSurfaceVariant,
    surfaceContainerLowest = Signal_SurfaceContainerLowest,
    surfaceContainerLow = Signal_SurfaceContainerLow,
    surfaceContainer = Signal_SurfaceContainer,
    surfaceContainerHigh = Signal_SurfaceContainerHigh,
    surfaceContainerHighest = Signal_SurfaceContainerHighest,
    outline = Signal_Outline,
    outlineVariant = Signal_OutlineVariant,
    inverseSurface = Signal_InverseSurface,
    inverseOnSurface = Signal_InverseOnSurface,
    inversePrimary = Signal_InversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = Signal_PrimaryDark,
    onPrimary = Signal_OnPrimaryDark,
    primaryContainer = Signal_PrimaryContainerDark,
    onPrimaryContainer = Signal_OnPrimaryContainerDark,
    secondary = Signal_SecondaryDark,
    onSecondary = Signal_OnSecondaryDark,
    secondaryContainer = Signal_SecondaryContainerDark,
    onSecondaryContainer = Signal_OnSecondaryContainerDark,
    tertiary = Signal_TertiaryDark,
    onTertiary = Signal_OnTertiaryDark,
    tertiaryContainer = Signal_TertiaryContainerDark,
    onTertiaryContainer = Signal_OnTertiaryContainerDark,
    error = Signal_ErrorDark,
    onError = Signal_OnErrorDark,
    errorContainer = Signal_ErrorContainerDark,
    onErrorContainer = Signal_OnErrorContainerDark,
    background = Signal_SurfaceDark,
    onBackground = Signal_OnSurfaceDark,
    surface = Signal_SurfaceDark,
    onSurface = Signal_OnSurfaceDark,
    surfaceVariant = Signal_SurfaceVariantDark,
    onSurfaceVariant = Signal_OnSurfaceVariantDark,
    surfaceContainerLowest = Signal_SurfaceContainerLowestDark,
    surfaceContainerLow = Signal_SurfaceContainerLowDark,
    surfaceContainer = Signal_SurfaceContainerDark,
    surfaceContainerHigh = Signal_SurfaceContainerHighDark,
    surfaceContainerHighest = Signal_SurfaceContainerHighestDark,
    outline = Signal_OutlineDark,
    outlineVariant = Signal_OutlineVariantDark,
    inverseSurface = Signal_InverseSurfaceDark,
    inverseOnSurface = Signal_InverseOnSurfaceDark,
    inversePrimary = Signal_InversePrimaryDark,
)

@Composable
fun ChatAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — we enforce the Señal palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // enableEdgeToEdge() only styles the system bar icons from the *system* light/dark
    // setting at launch — it has no way to know about our in-app ThemePreference
    // (SYSTEM/LIGHT/DARK), which can diverge from the system setting or change at
    // runtime without recreating the Activity. Re-assert the correct icon appearance
    // on every recomposition so it always matches what's actually on screen.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ChatAppShapes,
        typography = ChatAppTypography,
        content = content,
    )
}
