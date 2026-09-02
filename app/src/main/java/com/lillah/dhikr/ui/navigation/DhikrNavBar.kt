package com.lillah.dhikr.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lillah.dhikr.ui.components.softShadow
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion
import com.lillah.dhikr.ui.theme.Radii

/**
 * A floating bar rather than an edge-to-edge one: it keeps the aurora visible underneath and lets
 * the selected item lift into a filled pill instead of relying on a thin indicator line.
 */
@Composable
fun DhikrNavBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradients = LocalAppGradients.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .softShadow(20.dp, Radii.chip, MaterialTheme.colorScheme.primary, alpha = 0.22f)
            .clip(Radii.chip)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), Radii.chip)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.92f,
                animationSpec = Motion.bouncy(),
                label = "navScale_${destination.route}",
            )
            val tint by animateColorAsState(
                targetValue = if (selected) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "navTint_${destination.route}",
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.chip)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(destination) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(width = 40.dp, height = 30.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) {
                                Modifier.background(Brush.linearGradient(gradients.hero))
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (selected) {
                            destination.selectedIcon
                        } else {
                            destination.icon
                        },
                        contentDescription = destination.label,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    maxLines = 1,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
