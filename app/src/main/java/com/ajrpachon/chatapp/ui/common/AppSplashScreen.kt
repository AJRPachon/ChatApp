package com.ajrpachon.chatapp.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.theme.Signal_OnSurface
import com.ajrpachon.chatapp.ui.theme.Signal_OnSurfaceDark
import com.ajrpachon.chatapp.ui.theme.Signal_OnSurfaceVariant
import com.ajrpachon.chatapp.ui.theme.Signal_OnSurfaceVariantDark
import com.ajrpachon.chatapp.ui.theme.Signal_Primary
import com.ajrpachon.chatapp.ui.theme.Signal_Surface
import com.ajrpachon.chatapp.ui.theme.Signal_SurfaceDark

/**
 * The "Splash · oscuro y claro" section of the Opción I design, verbatim — icon, "ChatApp"
 * wordmark with a blinking cursor, and tagline, on the design's own background/text colors
 * (which is exactly [Signal_SurfaceDark]/[Signal_OnSurfaceDark]/[Signal_OnSurfaceVariantDark] in
 * dark and [Signal_Surface]/[Signal_OnSurface]/[Signal_OnSurfaceVariant] in light — the theme
 * tokens were lifted from this design in the first place).
 *
 * Shown by [com.ajrpachon.chatapp.MainActivity] while the Play Integrity check and the initial
 * route resolve. The system's own SplashScreen (see `installSplashScreen()` there) still covers
 * the very first instant of cold start — that gap is unavoidable — but it is dismissed the
 * moment this Composable's first frame is ready, not after those checks finish, so it hands off
 * directly into this screen with nothing shown in between.
 *
 * The icon is pinned to the screen's exact geometric center — NOT centered as a group with the
 * text below it — to match the system SplashScreen's own icon (`ic_launcher_foreground_inset`,
 * see themes.xml — the platform fixes its position at that exact point, with no API to move it).
 * [iconSize] is tuned so the rendered content matches that icon's size too (measured on device).
 * It's drawn straight from `ic_launcher_foreground.xml` (same drawable as the launcher/system
 * splash icon) rather than a hand-drawn Composable — the badge accent is fixed regardless of
 * theme (`#6C9BD1`, the dark-theme blue), which happens to be an acceptable tradeoff here since
 * this screen doesn't need to distinguish itself further. The tradeoff of pinning the icon rather
 * than centering icon+text as one group: that reads slightly bottom-heavy rather than perfectly
 * centered, since the icon can't shift down to balance the text below it.
 */
@Composable
fun AppSplashScreen(darkTheme: Boolean, modifier: Modifier = Modifier) {
    val background = if (darkTheme) Signal_SurfaceDark else Signal_Surface
    val onBackground = if (darkTheme) Signal_OnSurfaceDark else Signal_OnSurface
    val muted = if (darkTheme) Signal_OnSurfaceVariantDark else Signal_OnSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(iconSize).align(Alignment.Center),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = iconSize / 2 + 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ChatApp",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = onBackground,
                )
                BlinkingCursor()
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "cifrado extremo a extremo",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
        }
    }
}

// Matches the system SplashScreen's own icon size exactly — see the class doc above. Measured
// on device: Phase 1 (system icon) renders at 364x360px; ic_launcher_foreground.xml's drawn
// content fills ~78% of its own 108-unit viewport, so a 170dp Image (467px @ 2.75px/dp density)
// reproduces that same 364px rendered size.
private val iconSize = 170.dp

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "splash_cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash_cursor_alpha",
    )
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .width(8.dp)
            .height(30.dp)
            .background(Signal_Primary.copy(alpha = alpha)),
    )
}
