package com.lillah.dhikr.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.ui.components.softShadow
import com.lillah.dhikr.ui.theme.Motion
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import com.lillah.dhikr.ui.theme.ThemeMode
import com.lillah.dhikr.ui.theme.ThemePalette

/**
 * The theme picker is a gallery, not a list of names.
 *
 * Each card paints itself in its own palette rather than the active one, so choosing is a matter
 * of looking rather than reading — and because the whole app cross-fades between schemes, tapping
 * one shows the real result immediately behind the picker.
 */
@Composable
fun ThemeGallery(
    selected: ThemePalette,
    dark: Boolean,
    onSelect: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        items(ThemePalette.entries.size) { index ->
            val palette = ThemePalette.entries[index]
            PaletteCard(
                palette = palette,
                dark = dark,
                selected = palette == selected,
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun PaletteCard(
    palette: ThemePalette,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spec = palette.spec(dark)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        animationSpec = Motion.bouncy(),
        label = "paletteScale_${palette.key}",
    )

    Column(
        modifier = Modifier
            .width(152.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp)
                .softShadow(
                    elevation = if (selected) 18.dp else 8.dp,
                    shape = Radii.cover,
                    color = spec.heroStops.first(),
                    alpha = if (selected) 0.45f else 0.22f,
                )
                .clip(Radii.cover)
                .background(Brush.linearGradient(spec.heroStops))
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, Radii.cover)
                    } else {
                        Modifier
                    }
                ),
        ) {
            // Miniature of the app's own counter, painted in this palette.
            Canvas(Modifier.fillMaxSize().padding(Spacing.l)) {
                val radius = size.minDimension * 0.34f
                val stroke = size.minDimension * 0.085f
                drawArc(
                    color = Color.White.copy(alpha = 0.32f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - radius,
                        center.y - radius,
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 254f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - radius,
                        center.y - radius,
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = radius * 0.62f,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                listOf(spec.primary, spec.secondary, spec.tertiary).forEach { swatch ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.s)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        "Selected",
                        tint = spec.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.s))
        Text(
            text = palette.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = palette.tagline,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        ThemeMode.entries.forEach { mode ->
            val icon: ImageVector = when (mode) {
                ThemeMode.Light -> Icons.Rounded.LightMode
                ThemeMode.Dark -> Icons.Rounded.DarkMode
                ThemeMode.System -> Icons.Rounded.PhoneAndroid
            }
            val isSelected = mode == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.tile)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        },
                        shape = Radii.tile,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(mode) }
                    .padding(vertical = Spacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
