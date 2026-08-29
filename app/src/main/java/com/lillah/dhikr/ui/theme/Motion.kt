package com.lillah.dhikr.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * One motion vocabulary for the whole app. Springs for anything the finger drives, eased tweens
 * for anything the app drives on its own.
 */
object Motion {
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)
    val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val Quick = 180
    const val Medium = 320
    const val Slow = 520
    const val Celebration = 1100

    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)

    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)

    fun <T> emphasized(durationMillis: Int = Medium): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = Emphasized)
}
