package com.lillah.dhikr.ui.screens.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Spacing

@Composable
fun ManageDhikrScreen(
    state: ManageUiState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onArchive: (Dhikr) -> Unit,
    onRestore: (Dhikr) -> Unit,
    onMove: (Dhikr, Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                    Spacer(Modifier.size(Spacing.m))
                    Text(
                        text = "Manage adhkar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.weight(1f))
                    RoundButton(Icons.Rounded.Add, "Add a dhikr", onAdd)
                }
                Spacer(Modifier.height(Spacing.l))
                Text(
                    text = "Tap to edit. Use the arrows to change the order they appear in " +
                        "beneath the counter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.l))
            }
        }

        items(state.active, key = { it.id }) { dhikr ->
            Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)) {
                ManageRow(
                    dhikr = dhikr,
                    collectionName = state.collections
                        .firstOrNull { it.id == dhikr.collectionId }?.name,
                    onEdit = { onEdit(dhikr.id) },
                    onToggleFavorite = { onToggleFavorite(dhikr) },
                    onArchive = { onArchive(dhikr) },
                    onMoveUp = { onMove(dhikr, -1) },
                    onMoveDown = { onMove(dhikr, 1) },
                )
            }
        }

        if (state.archived.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = Spacing.screen)) {
                    Spacer(Modifier.height(Spacing.xl))
                    SectionHeader(
                        title = "Archived",
                        subtitle = "Out of the way, history intact",
                    )
                    Spacer(Modifier.height(Spacing.m))
                }
            }
            items(state.archived, key = { "archived_${it.id}" }) { dhikr ->
                Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)) {
                    SoftCard(
                        Modifier.fillMaxWidth(),
                        contentPadding = Spacing.l,
                        elevation = 3.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dhikr.displayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            RoundButton(Icons.Rounded.Restore, "Restore", onClick = { onRestore(dhikr) })
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun ManageRow(
    dhikr: Dhikr,
    collectionName: String?,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val gradients = LocalAppGradients.current

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = Spacing.l,
        elevation = 6.dp,
        onClick = onEdit,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradients.accent(dhikr.accentIndex)))
            )
            Spacer(Modifier.size(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    text = dhikr.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("×${dhikr.targetCount}")
                        if (collectionName != null) append(" · $collectionName")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoundButton(
                icon = if (dhikr.isFavorite) {
                    Icons.Rounded.Favorite
                } else {
                    Icons.Rounded.FavoriteBorder
                },
                description = if (dhikr.isFavorite) "Remove from favourites" else "Add to favourites",
                tint = if (dhikr.isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onToggleFavorite,
            )
        }

        Spacer(Modifier.height(Spacing.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            RoundButton(Icons.Rounded.ArrowUpward, "Move up", onClick = onMoveUp)
            RoundButton(Icons.Rounded.ArrowDownward, "Move down", onClick = onMoveDown)
            Spacer(Modifier.weight(1f))
            RoundButton(Icons.Rounded.Inventory2, "Archive", onClick = onArchive)
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
