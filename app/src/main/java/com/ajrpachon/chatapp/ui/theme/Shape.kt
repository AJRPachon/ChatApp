package com.ajrpachon.chatapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * "Señal" shape scale: corners close to square rather than the fully rounded pills used
 * elsewhere in Material You. Components read this via [androidx.compose.material3.MaterialTheme.shapes]
 * — change a value here and every button, field, bubble and FAB that references it follows.
 */
val ChatAppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Corner sizes the Material scale above does not have. Kept here so a screen never has to spell a
 * corner radius itself.
 */
object ChatAppShapeExtras {
    /** Thumbnails, quoted-message previews and skeleton blocks. */
    val Thumbnail = RoundedCornerShape(6.dp)

    /** Thin progress segments (story viewer). */
    val ProgressSegment = RoundedCornerShape(2.dp)

    /** Top corners only, for a panel that sits on the edge of the screen. */
    val PanelTop = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)

    /** Sent message bubble: the corner next to the sender is tighter. */
    val BubbleSent = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    /** Received message bubble. */
    val BubbleReceived = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
}
