package com.lillah.dhikr.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Straight sRGB blend. Good enough — and predictable — for the soft, low-chroma mixes here. */
fun Color.mix(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t,
    )
}

fun Color.lighten(amount: Float): Color = mix(Color.White, amount)
fun Color.darken(amount: Float): Color = mix(Color.Black, amount)

// Neither pure black nor pure white. Ink carries a trace of the palette so text sits in the same
// family as everything around it, and the dark canvas is deep enough for colour to read as light
// against it rather than as grey.
private val LightInk = Color(0xFF12111A)
private val DarkCanvas = Color(0xFF07060D)
private val DarkInk = Color(0xFFF2EFFA)

private val ErrorLight = Color(0xFFBA1A1A)
private val ErrorDark = Color(0xFFFFB4AB)

/**
 * Derives a full Material 3 scheme from the five palette seeds. Light schemes keep surfaces
 * near-white with a whisper of the primary hue; dark schemes sit on a near-black canvas lifted
 * slightly toward the palette so the app never looks like grey-on-grey.
 */
fun PaletteSpec.toColorScheme(dark: Boolean): ColorScheme = if (!dark) {
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.lighten(0.88f),
        onPrimaryContainer = primary.darken(0.42f),
        inversePrimary = primary.lighten(0.58f),

        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.lighten(0.89f),
        onSecondaryContainer = secondary.darken(0.42f),

        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = tertiary.lighten(0.88f),
        onTertiaryContainer = tertiary.darken(0.42f),

        background = auroraStops.first().mix(Color.White, 0.955f),
        onBackground = LightInk.mix(primary, 0.08f),
        surface = primary.mix(Color.White, 0.978f),
        onSurface = LightInk.mix(primary, 0.08f),
        surfaceVariant = primary.lighten(0.92f),
        onSurfaceVariant = primary.darken(0.28f).mix(LightInk, 0.5f),
        surfaceTint = primary,
        inverseSurface = LightInk.mix(primary, 0.12f),
        inverseOnSurface = Color.White,

        // Six clearly separated steps rather than four near-identical ones: depth in a light
        // theme comes from surfaces that are actually distinguishable, not from heavier shadows.
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = primary.mix(Color.White, 0.982f),
        surfaceContainer = primary.mix(Color.White, 0.962f),
        surfaceContainerHigh = primary.mix(Color.White, 0.932f),
        surfaceContainerHighest = primary.mix(Color.White, 0.9f),

        outline = primary.lighten(0.56f),
        outlineVariant = primary.lighten(0.87f),
        scrim = LightInk,

        error = ErrorLight,
        onError = Color.White,
        errorContainer = ErrorLight.lighten(0.87f),
        onErrorContainer = ErrorLight.darken(0.38f),
    )
} else {
    darkColorScheme(
        primary = primary,
        onPrimary = primary.darken(0.78f),
        primaryContainer = primary.mix(DarkCanvas, 0.72f),
        onPrimaryContainer = primary.lighten(0.62f),
        inversePrimary = primary.darken(0.32f),

        secondary = secondary,
        onSecondary = secondary.darken(0.78f),
        secondaryContainer = secondary.mix(DarkCanvas, 0.74f),
        onSecondaryContainer = secondary.lighten(0.62f),

        tertiary = tertiary,
        onTertiary = tertiary.darken(0.78f),
        tertiaryContainer = tertiary.mix(DarkCanvas, 0.74f),
        onTertiaryContainer = tertiary.lighten(0.62f),

        background = primary.mix(DarkCanvas, 0.955f),
        onBackground = DarkInk,
        surface = primary.mix(DarkCanvas, 0.94f),
        onSurface = DarkInk,
        surfaceVariant = primary.mix(DarkCanvas, 0.86f),
        onSurfaceVariant = primary.lighten(0.42f).mix(DarkInk, 0.45f),
        surfaceTint = primary,
        inverseSurface = DarkInk,
        inverseOnSurface = DarkCanvas,

        surfaceContainerLowest = primary.mix(DarkCanvas, 0.975f),
        surfaceContainerLow = primary.mix(DarkCanvas, 0.94f),
        surfaceContainer = primary.mix(DarkCanvas, 0.915f),
        surfaceContainerHigh = primary.mix(DarkCanvas, 0.875f),
        surfaceContainerHighest = primary.mix(DarkCanvas, 0.82f),

        outline = primary.mix(DarkCanvas, 0.6f),
        outlineVariant = primary.mix(DarkCanvas, 0.82f),
        scrim = Color.Black,

        error = ErrorDark,
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}

/**
 * Six recurring accent hues used to give individual adhkar their own identity. Each is pulled a
 * little toward the active palette so a list of adhkar still reads as one family.
 */
val AccentSeeds: List<Color> = listOf(
    Color(0xFF5B8CFF),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFFF59E0B),
    Color(0xFF10B981),
    Color(0xFF06B6D4),
)

fun accentGradient(index: Int, spec: PaletteSpec, dark: Boolean): List<Color> {
    val seed = AccentSeeds[((index % AccentSeeds.size) + AccentSeeds.size) % AccentSeeds.size]
    val start = seed.mix(spec.primary, 0.28f)
    val end = seed.mix(spec.heroStops.last(), 0.42f)
    return if (dark) {
        listOf(start.darken(0.18f), end.darken(0.24f))
    } else {
        listOf(start, end)
    }
}
