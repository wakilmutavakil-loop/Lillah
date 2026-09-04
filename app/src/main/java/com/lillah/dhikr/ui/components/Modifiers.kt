package com.lillah.dhikr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion

/**
 * Tinted, wide, low-opacity shadow. Material's default black shadow reads as grime under the
 * pastel surfaces here; colouring it to the content keeps the depth and loses the dirt.
 */
fun Modifier.softShadow(
    elevation: Dp,
    shape: Shape,
    color: Color,
    alpha: Float = 0.24f,
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = alpha * 0.7f),
    spotColor = color.copy(alpha = alpha),
)

/**
 * A soft top-light over a gradient surface.
 *
 * Drawn between the background and the content, so it lifts the colour without washing out the
 * text sitting on it. This is most of the difference between a flat coloured rectangle and
 * something that looks like it has a light source.
 */
fun Modifier.sheenOverlay(): Modifier = composed {
    val gradients = LocalAppGradients.current
    drawWithContent {
        drawRect(Brush.verticalGradient(gradients.sheen))
        drawContent()
    }
}

/** The frosted panel used for cards floating over the aurora background. */
fun Modifier.glassSurface(shape: Shape, borderWidth: Dp = 1.dp): Modifier = composed {
    val gradients = LocalAppGradients.current
    this
        .background(gradients.glass, shape)
        .border(borderWidth, gradients.glassBorder, shape)
}

/** Scales down while held. Applied to anything that should feel physically pressed. */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.bouncy(),
        label = "pressScale",
    )
    return scale
}

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val source = remember { interactionSource }
    scale(rememberPressScale(source, pressedScale))
}
