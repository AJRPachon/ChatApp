package com.ajrpachon.chatapp.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a composable in light and dark theme. [com.ajrpachon.chatapp.ui.theme.ChatAppTheme]
 * follows the system `uiMode` by default, so the previews only need to be wrapped in the theme.
 */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/** Checks that text-heavy components survive a large system font scale. */
@Preview(name = "Font 1.5x", showBackground = true, fontScale = 1.5f)
annotation class FontScalePreviews
