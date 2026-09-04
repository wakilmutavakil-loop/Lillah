package com.lillah.dhikr.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A palette is authored as a handful of seed colours. Everything Material needs — containers,
 * surfaces, outlines, on-colours — is derived from those seeds by [PaletteSpec.toColorScheme],
 * so adding a theme means picking five colours rather than tuning forty.
 */
@Immutable
data class PaletteSpec(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    /** Stops for the hero surfaces: the counter ring, featured cards, the celebration bloom. */
    val heroStops: List<Color>,
    /** Stops for the ambient background wash sitting behind every screen. */
    val auroraStops: List<Color>,
)

@Immutable
enum class ThemePalette(
    val key: String,
    val displayName: String,
    val tagline: String,
    val light: PaletteSpec,
    val dark: PaletteSpec,
) {
    Copilot(
        key = "copilot",
        displayName = "Copilot",
        tagline = "Electric indigo drifting into violet",
        light = PaletteSpec(
            primary = Color(0xFF3D4FE0),
            secondary = Color(0xFF7C4DFF),
            tertiary = Color(0xFF00B8D4),
            heroStops = listOf(Color(0xFF3D5AFE), Color(0xFF7C4DFF), Color(0xFFC158FC)),
            auroraStops = listOf(Color(0xFF5B7CFF), Color(0xFF9D7BFF), Color(0xFF4DD0E1)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF8C9EFF),
            secondary = Color(0xFFB388FF),
            tertiary = Color(0xFF4DD0E1),
            heroStops = listOf(Color(0xFF2A3EDB), Color(0xFF5E35D4), Color(0xFF9C27B0)),
            auroraStops = listOf(Color(0xFF2F44C7), Color(0xFF6236C4), Color(0xFF0E7C8C)),
        ),
    ),
    Jade(
        key = "emerald",
        displayName = "Jade",
        tagline = "Still water over polished stone",
        light = PaletteSpec(
            primary = Color(0xFF00875A),
            secondary = Color(0xFF0E9E93),
            tertiary = Color(0xFF57A639),
            heroStops = listOf(Color(0xFF00A870), Color(0xFF00B3A6), Color(0xFF6EE7B7)),
            auroraStops = listOf(Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFF86EFAC)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF4ADE9B),
            secondary = Color(0xFF3FD5C7),
            tertiary = Color(0xFF9BD75A),
            heroStops = listOf(Color(0xFF04724F), Color(0xFF06776F), Color(0xFF1F9D6B)),
            auroraStops = listOf(Color(0xFF0A7D57), Color(0xFF07716A), Color(0xFF2E8B5C)),
        ),
    ),
    Sunset(
        key = "sunset",
        displayName = "Sunset",
        tagline = "The last warm light of the day",
        light = PaletteSpec(
            primary = Color(0xFFD9480F),
            secondary = Color(0xFFD97706),
            tertiary = Color(0xFFDB2777),
            heroStops = listOf(Color(0xFFFF6B35), Color(0xFFF7931E), Color(0xFFEC4899)),
            auroraStops = listOf(Color(0xFFFDBA74), Color(0xFFFCA5A5), Color(0xFFF9A8D4)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFFF9E7A),
            secondary = Color(0xFFFFC062),
            tertiary = Color(0xFFFF80AB),
            heroStops = listOf(Color(0xFFB03A0C), Color(0xFFB56A08), Color(0xFF9D174D)),
            auroraStops = listOf(Color(0xFFA84318), Color(0xFFAA6316), Color(0xFF96235B)),
        ),
    ),
    Orchid(
        key = "lavender",
        displayName = "Orchid",
        tagline = "Violet deepening into magenta",
        light = PaletteSpec(
            primary = Color(0xFF7C3AED),
            secondary = Color(0xFFA21CAF),
            tertiary = Color(0xFFC026D3),
            heroStops = listOf(Color(0xFF7C3AED), Color(0xFFC026D3), Color(0xFFF0ABFC)),
            auroraStops = listOf(Color(0xFFC4B5FD), Color(0xFFE9A8FB), Color(0xFFF5D0FE)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFC4A6FF),
            secondary = Color(0xFFE879F9),
            tertiary = Color(0xFFF0ABFC),
            heroStops = listOf(Color(0xFF5B21B6), Color(0xFF86198F), Color(0xFFA21CAF)),
            auroraStops = listOf(Color(0xFF5C2CB0), Color(0xFF83218C), Color(0xFF9D2BA8)),
        ),
    ),
    Azure(
        key = "ocean",
        displayName = "Azure",
        tagline = "Deep calm under an open sky",
        light = PaletteSpec(
            primary = Color(0xFF0369A1),
            secondary = Color(0xFF1D4ED8),
            tertiary = Color(0xFF0891B2),
            heroStops = listOf(Color(0xFF0EA5E9), Color(0xFF2563EB), Color(0xFF22D3EE)),
            auroraStops = listOf(Color(0xFF7DD3FC), Color(0xFF93C5FD), Color(0xFFA5F3FC)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF60C7F5),
            secondary = Color(0xFF7FA6FF),
            tertiary = Color(0xFF4FD8E8),
            heroStops = listOf(Color(0xFF075985), Color(0xFF1E3A8A), Color(0xFF0E7490)),
            auroraStops = listOf(Color(0xFF0A6A9E), Color(0xFF23459C), Color(0xFF107E93)),
        ),
    ),
    Graphite(
        key = "monochrome",
        displayName = "Graphite",
        tagline = "Nothing but structure and space",
        light = PaletteSpec(
            primary = Color(0xFF3F4654),
            secondary = Color(0xFF5A6272),
            tertiary = Color(0xFF7C8494),
            heroStops = listOf(Color(0xFF434A59), Color(0xFF646D80), Color(0xFF98A1B2)),
            auroraStops = listOf(Color(0xFF9AA3B2), Color(0xFFB4BCC8), Color(0xFFD4D9E0)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFCBD2DE),
            secondary = Color(0xFFA5ADBB),
            tertiary = Color(0xFF858D9C),
            heroStops = listOf(Color(0xFF2D323D), Color(0xFF434A59), Color(0xFF636B7C)),
            auroraStops = listOf(Color(0xFF373D49), Color(0xFF4C5464), Color(0xFF666F80)),
        ),
    ),
    Midnight(
        key = "midnight",
        displayName = "Midnight",
        tagline = "Indigo night, struck with gold",
        light = PaletteSpec(
            primary = Color(0xFF3730A3),
            secondary = Color(0xFF9A7B18),
            tertiary = Color(0xFF4F46E5),
            heroStops = listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFFD4AF37)),
            auroraStops = listOf(Color(0xFF6366F1), Color(0xFF8B7BE8), Color(0xFFE3C77B)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF9BA3FF),
            secondary = Color(0xFFE8C871),
            tertiary = Color(0xFFA5A8FF),
            heroStops = listOf(Color(0xFF161344), Color(0xFF322BA8), Color(0xFFB08D24)),
            auroraStops = listOf(Color(0xFF272272), Color(0xFF423AB4), Color(0xFF8A6D1C)),
        ),
    ),
    Rose(
        key = "rose",
        displayName = "Rose",
        tagline = "Deep rose softening to blush",
        light = PaletteSpec(
            primary = Color(0xFFA81345),
            secondary = Color(0xFFC81E5B),
            tertiary = Color(0xFFE0526F),
            heroStops = listOf(Color(0xFF9F1239), Color(0xFFE11D48), Color(0xFFFDA4AF)),
            auroraStops = listOf(Color(0xFFFDA4AF), Color(0xFFF9A8D4), Color(0xFFFECDD3)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFFF9BAE),
            secondary = Color(0xFFFB7185),
            tertiary = Color(0xFFF9A8D4),
            heroStops = listOf(Color(0xFF6B0A26), Color(0xFF9F1239), Color(0xFFBE2C52)),
            auroraStops = listOf(Color(0xFF7C1233), Color(0xFFA31742), Color(0xFFC03A5F)),
        ),
    ),
    Bronze(
        key = "bronze",
        displayName = "Bronze",
        tagline = "Warm metal and desert sand",
        light = PaletteSpec(
            primary = Color(0xFF8C5A21),
            secondary = Color(0xFFB4761F),
            tertiary = Color(0xFFC79A3C),
            heroStops = listOf(Color(0xFF78350F), Color(0xFFB45309), Color(0xFFE3B341)),
            auroraStops = listOf(Color(0xFFE9C89A), Color(0xFFF0D9A8), Color(0xFFF6E7C8)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFE7BC79),
            secondary = Color(0xFFD9A455),
            tertiary = Color(0xFFC79A3C),
            heroStops = listOf(Color(0xFF4A2308), Color(0xFF7C3D06), Color(0xFFAE7C1E)),
            auroraStops = listOf(Color(0xFF5B2E0B), Color(0xFF8A4A0A), Color(0xFFA97C22)),
        ),
    );

    fun spec(dark: Boolean): PaletteSpec = if (dark) this.dark else this.light

    companion object {
        val Default = Copilot
        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.key == key } ?: Default
    }
}

enum class ThemeMode(val key: String, val label: String) {
    Light("light", "Light"),
    Dark("dark", "Dark"),
    System("system", "System");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}
