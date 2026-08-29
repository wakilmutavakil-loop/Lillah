package com.lillah.dhikr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Generously rounded throughout — nothing in the app has a sharp corner. */
val DhikrShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

object Radii {
    val chip = RoundedCornerShape(999.dp)
    val card = RoundedCornerShape(26.dp)
    val cardLarge = RoundedCornerShape(32.dp)
    val cover = RoundedCornerShape(30.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val tile = RoundedCornerShape(22.dp)
    val field = RoundedCornerShape(18.dp)
}

object Spacing {
    val hairline = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val screen = 20.dp
    /** Clearance for the floating navigation bar at the bottom of scrolling content. */
    val navClearance = 108.dp
}
