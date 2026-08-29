package com.lillah.dhikr.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Palette-derived surfaces that Material's own scheme has no slot for. */
@Immutable
data class AppGradients(
    val hero: List<Color>,
    val aurora: List<Color>,
    val accents: List<List<Color>>,
    val onHero: Color,
    val glass: Color,
    val glassBorder: Color,
    val isDark: Boolean,
) {
    fun accent(index: Int): List<Color> =
        accents[((index % accents.size) + accents.size) % accents.size]
}

val LocalAppGradients = staticCompositionLocalOf {
    gradientsFor(ThemePalette.Default.light, dark = false)
}

val LocalThemePalette = staticCompositionLocalOf { ThemePalette.Default }

fun gradientsFor(spec: PaletteSpec, dark: Boolean) = AppGradients(
    hero = spec.heroStops,
    aurora = spec.auroraStops,
    accents = List(AccentSeeds.size) { accentGradient(it, spec, dark) },
    onHero = Color.White,
    glass = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.66f),
    glassBorder = if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.75f),
    isDark = dark,
)

@Composable
private fun animColor(target: Color, label: String): Color {
    val value by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 520, easing = Motion.Standard),
        label = label,
    )
    return value
}

/**
 * Colours cross-fade between palettes rather than snapping, so switching a theme in Settings
 * reads as the whole app easing into a new mood.
 */
@Composable
private fun ColorScheme.animated(): ColorScheme = copy(
    primary = animColor(primary, "primary"),
    onPrimary = animColor(onPrimary, "onPrimary"),
    primaryContainer = animColor(primaryContainer, "primaryContainer"),
    onPrimaryContainer = animColor(onPrimaryContainer, "onPrimaryContainer"),
    secondary = animColor(secondary, "secondary"),
    onSecondary = animColor(onSecondary, "onSecondary"),
    secondaryContainer = animColor(secondaryContainer, "secondaryContainer"),
    onSecondaryContainer = animColor(onSecondaryContainer, "onSecondaryContainer"),
    tertiary = animColor(tertiary, "tertiary"),
    onTertiary = animColor(onTertiary, "onTertiary"),
    tertiaryContainer = animColor(tertiaryContainer, "tertiaryContainer"),
    onTertiaryContainer = animColor(onTertiaryContainer, "onTertiaryContainer"),
    background = animColor(background, "background"),
    onBackground = animColor(onBackground, "onBackground"),
    surface = animColor(surface, "surface"),
    onSurface = animColor(onSurface, "onSurface"),
    surfaceVariant = animColor(surfaceVariant, "surfaceVariant"),
    onSurfaceVariant = animColor(onSurfaceVariant, "onSurfaceVariant"),
    surfaceContainerLowest = animColor(surfaceContainerLowest, "scLowest"),
    surfaceContainerLow = animColor(surfaceContainerLow, "scLow"),
    surfaceContainer = animColor(surfaceContainer, "sc"),
    surfaceContainerHigh = animColor(surfaceContainerHigh, "scHigh"),
    surfaceContainerHighest = animColor(surfaceContainerHighest, "scHighest"),
    outline = animColor(outline, "outline"),
    outlineVariant = animColor(outlineVariant, "outlineVariant"),
)

@Composable
fun DhikrTheme(
    palette: ThemePalette = ThemePalette.Default,
    mode: ThemeMode = ThemeMode.System,
    animateColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }

    val spec = palette.spec(dark)
    val baseScheme = remember(palette, dark) { spec.toColorScheme(dark) }
    val scheme = if (animateColors) baseScheme.animated() else baseScheme
    val gradients = remember(palette, dark) { gradientsFor(spec, dark) }

    CompositionLocalProvider(
        LocalAppGradients provides gradients,
        LocalThemePalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DhikrTypography,
            shapes = DhikrShapes,
            content = content,
        )
    }
}
