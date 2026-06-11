package com.ajrpachon.chatapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Pastel_Primary,
    onPrimary = Pastel_OnPrimary,
    primaryContainer = Pastel_PrimaryContainer,
    onPrimaryContainer = Pastel_OnPrimaryContainer,
    secondary = Pastel_Secondary,
    onSecondary = Pastel_OnSecondary,
    secondaryContainer = Pastel_SecondaryContainer,
    onSecondaryContainer = Pastel_OnSecondaryContainer,
    tertiary = Pastel_Tertiary,
    onTertiary = Pastel_OnTertiary,
    tertiaryContainer = Pastel_TertiaryContainer,
    onTertiaryContainer = Pastel_OnTertiaryContainer,
    error = Pastel_Error,
    onError = Pastel_OnError,
    errorContainer = Pastel_ErrorContainer,
    onErrorContainer = Pastel_OnErrorContainer,
    surface = Pastel_Surface,
    onSurface = Pastel_OnSurface,
    surfaceVariant = Pastel_SurfaceVariant,
    onSurfaceVariant = Pastel_OnSurfaceVariant,
    surfaceContainerLowest = Pastel_SurfaceContainerLowest,
    surfaceContainerLow = Pastel_SurfaceContainerLow,
    surfaceContainer = Pastel_SurfaceContainer,
    surfaceContainerHigh = Pastel_SurfaceContainerHigh,
    surfaceContainerHighest = Pastel_SurfaceContainerHighest,
    outline = Pastel_Outline,
    outlineVariant = Pastel_OutlineVariant,
    inverseSurface = Pastel_InverseSurface,
    inverseOnSurface = Pastel_InverseOnSurface,
    inversePrimary = Pastel_InversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = Pastel_PrimaryDark,
    onPrimary = Pastel_OnPrimaryDark,
    primaryContainer = Pastel_PrimaryContainerDark,
    onPrimaryContainer = Pastel_OnPrimaryContainerDark,
    secondary = Pastel_SecondaryDark,
    onSecondary = Pastel_OnSecondaryDark,
    secondaryContainer = Pastel_SecondaryContainerDark,
    onSecondaryContainer = Pastel_OnSecondaryContainerDark,
    tertiary = Pastel_TertiaryDark,
    onTertiary = Pastel_OnTertiaryDark,
    tertiaryContainer = Pastel_TertiaryContainerDark,
    onTertiaryContainer = Pastel_OnTertiaryContainerDark,
    error = Pastel_ErrorDark,
    onError = Pastel_OnErrorDark,
    errorContainer = Pastel_ErrorContainerDark,
    onErrorContainer = Pastel_OnErrorContainerDark,
    surface = Pastel_SurfaceDark,
    onSurface = Pastel_OnSurfaceDark,
    surfaceVariant = Pastel_SurfaceVariantDark,
    onSurfaceVariant = Pastel_OnSurfaceVariantDark,
    surfaceContainerLowest = Pastel_SurfaceContainerLowestDark,
    surfaceContainerLow = Pastel_SurfaceContainerLowDark,
    surfaceContainer = Pastel_SurfaceContainerDark,
    surfaceContainerHigh = Pastel_SurfaceContainerHighDark,
    surfaceContainerHighest = Pastel_SurfaceContainerHighestDark,
    outline = Pastel_OutlineDark,
    outlineVariant = Pastel_OutlineVariantDark,
    inverseSurface = Pastel_InverseSurfaceDark,
    inverseOnSurface = Pastel_InverseOnSurfaceDark,
    inversePrimary = Pastel_InversePrimaryDark,
)

@Composable
fun ChatAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — we enforce the pastel palette
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

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ChatAppShapes,
        content = content,
    )
}
