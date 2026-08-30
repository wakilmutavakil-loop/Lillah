package com.lillah.dhikr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.domain.gamification.AchievementIcon
import com.lillah.dhikr.domain.gamification.AchievementStatus
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing

fun AchievementIcon.vector(): ImageVector = when (this) {
    AchievementIcon.Sparkle -> Icons.Rounded.AutoAwesome
    AchievementIcon.Seedling -> Icons.Rounded.Spa
    AchievementIcon.Sunrise -> Icons.Rounded.WbTwilight
    AchievementIcon.Moon -> Icons.Rounded.NightsStay
    AchievementIcon.Flame -> Icons.Rounded.LocalFireDepartment
    AchievementIcon.Ring -> Icons.Rounded.DonutLarge
    AchievementIcon.Heart -> Icons.Rounded.FavoriteBorder
    AchievementIcon.Star -> Icons.Rounded.Star
    AchievementIcon.Mountain -> Icons.Rounded.Terrain
    AchievementIcon.Feather -> Icons.Rounded.Create
    AchievementIcon.Compass -> Icons.Rounded.Explore
    AchievementIcon.Bloom -> Icons.Rounded.LocalFlorist
}

/**
 * Locked milestones stay visible and show their progress, so the grid reads as a map of what is
 * ahead rather than a wall of question marks.
 */
@Composable
fun AchievementBadge(
    status: AchievementStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val gradients = LocalAppGradients.current
    val accent = gradients.accent(status.def.accentIndex)
    val unlocked = status.isUnlocked

    SoftCard(
        modifier = modifier,
        shape = Radii.tile,
        contentPadding = Spacing.l,
        elevation = if (unlocked) 8.dp else 3.dp,
        containerColor = if (unlocked) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
        },
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (unlocked) {
                        Modifier.background(Brush.linearGradient(accent))
                    } else {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = status.def.icon.vector(),
                contentDescription = null,
                tint = if (unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(Spacing.m))
        Text(
            text = status.def.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (unlocked) status.def.note else status.def.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!unlocked) {
            Spacer(Modifier.height(Spacing.m))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(Radii.chip)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(status.fraction)
                        .height(5.dp)
                        .clip(Radii.chip)
                        .background(Brush.horizontalGradient(accent))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${status.progress} of ${status.def.goal}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}
