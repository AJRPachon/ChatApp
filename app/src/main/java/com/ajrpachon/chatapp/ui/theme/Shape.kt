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
