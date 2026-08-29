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
        tagline = "Electric blue drifting into violet",
        light = PaletteSpec(
            primary = Color(0xFF4B63F0),
            secondary = Color(0xFF8B5CF6),
            tertiary = Color(0xFF16BDCA),
            heroStops = listOf(Color(0xFF5B8CFF), Color(0xFF7C5CFF), Color(0xFFB05CFF)),
            auroraStops = listOf(Color(0xFF6D8BFF), Color(0xFFA78BFA), Color(0xFF67E8F9)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF8FA4FF),
            secondary = Color(0xFFB79BFF),
            tertiary = Color(0xFF5FE3EF),
            heroStops = listOf(Color(0xFF3C63E8), Color(0xFF6B4BE8), Color(0xFF9B4BD6)),
            auroraStops = listOf(Color(0xFF3E56D8), Color(0xFF7C5CE0), Color(0xFF2AA9C4)),
        ),
    ),
    Emerald(
        key = "emerald",
        displayName = "Emerald",
        tagline = "Still water and new leaves",
        light = PaletteSpec(
            primary = Color(0xFF0E9F7E),
            secondary = Color(0xFF14A8A0),
            tertiary = Color(0xFF6BAF2E),
            heroStops = listOf(Color(0xFF14BE8C), Color(0xFF0EA5A5), Color(0xFF4ADE9B)),
            auroraStops = listOf(Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFFA7F3D0)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF54D8AC),
            secondary = Color(0xFF4FD1C7),
            tertiary = Color(0xFF9BD75A),
            heroStops = listOf(Color(0xFF0E8F6C), Color(0xFF0B8A8A), Color(0xFF2FA97A)),
            auroraStops = listOf(Color(0xFF16A37A), Color(0xFF0E8C8C), Color(0xFF3FAE7C)),
        ),
    ),
    Sunset(
        key = "sunset",
        displayName = "Sunset",
        tagline = "The last warm light of the day",
        light = PaletteSpec(
            primary = Color(0xFFE05A38),
            secondary = Color(0xFFE08A16),
            tertiary = Color(0xFFDB4A8C),
            heroStops = listOf(Color(0xFFFF7A45), Color(0xFFFB923C), Color(0xFFF472B6)),
            auroraStops = listOf(Color(0xFFFDBA74), Color(0xFFFCA5A5), Color(0xFFF9A8D4)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFFF9873),
            secondary = Color(0xFFFFBC5C),
            tertiary = Color(0xFFFF87BA),
            heroStops = listOf(Color(0xFFD1512F), Color(0xFFD1762B), Color(0xFFBE3F76)),
            auroraStops = listOf(Color(0xFFC65B34), Color(0xFFC4703A), Color(0xFFB44C7E)),
        ),
    ),
    Lavender(
        key = "lavender",
        displayName = "Lavender",
        tagline = "Soft violet, quiet and clear",
        light = PaletteSpec(
            primary = Color(0xFF7C5CE6),
            secondary = Color(0xFF9F4FE0),
            tertiary = Color(0xFFC55FD8),
            heroStops = listOf(Color(0xFF9B6BFF), Color(0xFFC084FC), Color(0xFFF0ABFC)),
            auroraStops = listOf(Color(0xFFC4B5FD), Color(0xFFDDA8FB), Color(0xFFF5D0FE)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFB79BFF),
            secondary = Color(0xFFCE9BF2),
            tertiary = Color(0xFFE3A8F0),
            heroStops = listOf(Color(0xFF6D4BD1), Color(0xFF9A55D6), Color(0xFFC062DC)),
            auroraStops = listOf(Color(0xFF6E4FC7), Color(0xFF8F55CE), Color(0xFFB05FD1)),
        ),
    ),
    Ocean(
        key = "ocean",
        displayName = "Ocean",
        tagline = "Deep calm and open sky",
        light = PaletteSpec(
            primary = Color(0xFF0C8FCE),
            secondary = Color(0xFF2563EB),
            tertiary = Color(0xFF06A6BE),
            heroStops = listOf(Color(0xFF38BDF8), Color(0xFF3B82F6), Color(0xFF22D3EE)),
            auroraStops = listOf(Color(0xFF7DD3FC), Color(0xFF93C5FD), Color(0xFFA5F3FC)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF5EC5F5),
            secondary = Color(0xFF7DA5FF),
            tertiary = Color(0xFF4FD8E8),
            heroStops = listOf(Color(0xFF0B78AE), Color(0xFF2B5FCC), Color(0xFF0E93A8)),
            auroraStops = listOf(Color(0xFF0E7FB4), Color(0xFF2E5EC4), Color(0xFF128FA3)),
        ),
    ),
    Monochrome(
        key = "monochrome",
        displayName = "Graphite",
        tagline = "Nothing but structure and space",
        light = PaletteSpec(
            primary = Color(0xFF4B5163),
            secondary = Color(0xFF6B7280),
            tertiary = Color(0xFF8A91A0),
            heroStops = listOf(Color(0xFF565D70), Color(0xFF7A8296), Color(0xFFA8AFBE)),
            auroraStops = listOf(Color(0xFF9CA3AF), Color(0xFFB6BCC7), Color(0xFFD1D5DB)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFFC3C9D6),
            secondary = Color(0xFFA8AFBE),
            tertiary = Color(0xFF8A91A0),
            heroStops = listOf(Color(0xFF3D4353), Color(0xFF565D70), Color(0xFF767E92)),
            auroraStops = listOf(Color(0xFF454B5C), Color(0xFF5A6174), Color(0xFF737B8E)),
        ),
    ),
    Midnight(
        key = "midnight",
        displayName = "Midnight",
        tagline = "Indigo night touched with gold",
        light = PaletteSpec(
            primary = Color(0xFF4F46E5),
            secondary = Color(0xFFB08528),
            tertiary = Color(0xFF6366F1),
            heroStops = listOf(Color(0xFF3F3AC4), Color(0xFF5B57E0), Color(0xFFD4A64A)),
            auroraStops = listOf(Color(0xFF818CF8), Color(0xFF9A8CF0), Color(0xFFE8C77A)),
        ),
        dark = PaletteSpec(
            primary = Color(0xFF9AA0FF),
            secondary = Color(0xFFE3BE6A),
            tertiary = Color(0xFFA5A8FF),
            heroStops = listOf(Color(0xFF2F2BA0), Color(0xFF4A46C4), Color(0xFFB08528)),
            auroraStops = listOf(Color(0xFF3A369F), Color(0xFF524EC0), Color(0xFF9C7526)),
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
