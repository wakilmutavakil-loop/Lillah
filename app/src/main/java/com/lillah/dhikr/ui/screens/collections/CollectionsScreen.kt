package com.lillah.dhikr.ui.screens.collections

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.ui.components.CollectionCoverCard
import com.lillah.dhikr.ui.components.EmptyState
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing

@Composable
fun CollectionsScreen(
    state: CollectionsUiState,
    onOpenCollection: (Long) -> Unit,
    onCreateCollection: () -> Unit,
    onManageAdhkar: () -> Unit,
    onSelectDhikr: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.s))
                SectionHeader(
                    title = "Adhkar",
                    subtitle = "${state.totalAdhkar} adhkar across your collections",
                    trailing = {
                        HeaderAction(Icons.Rounded.Tune, "Manage adhkar", onManageAdhkar)
                        Spacer(Modifier.size(Spacing.s))
                        HeaderAction(Icons.Rounded.Add, "New collection", onCreateCollection)
                    },
                )
                Spacer(Modifier.height(Spacing.l))
            }
        }

        items(state.featured, key = { it.collection.id }) { progress ->
            Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s)) {
                CollectionCoverCard(
                    progress = progress,
                    onClick = { onOpenCollection(progress.collection.id) },
                    height = 194.dp,
                )
            }
        }

        if (state.favorites.isNotEmpty()) {
            item {
                Column {
                    Spacer(Modifier.height(Spacing.l))
                    SectionHeader(
                        title = "Favourites",
                        subtitle = "Straight to the counter",
                        modifier = Modifier.padding(horizontal = Spacing.screen),
                    )
                    Spacer(Modifier.height(Spacing.m))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        items(state.favorites, key = { it.id }) { dhikr ->
                            FavoriteChip(dhikr = dhikr, onClick = { onSelectDhikr(dhikr.id) })
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.xl))
                SectionHeader(
                    title = "Collections",
                    subtitle = "Group adhkar the way you actually pray",
                )
                Spacer(Modifier.height(Spacing.m))
            }
        }

        // Two-up rows, built by hand so the whole screen stays one scrolling list.
        items(state.others.chunked(2)) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                row.forEach { progress ->
                    Box(Modifier.weight(1f)) {
                        CollectionCoverCard(
                            progress = progress,
                            onClick = { onOpenCollection(progress.collection.id) },
                            height = 142.dp,
                            shape = Radii.card,
                            compact = true,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s)) {
                NewCollectionTile(onClick = onCreateCollection)
            }
        }

        if (state.others.isEmpty() && state.featured.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    title = "No collections yet",
                    message = "Collections group adhkar you say together — a morning routine, " +
                        "something for after prayer, or anything of your own.",
                    icon = Icons.Rounded.Add,
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun HeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun FavoriteChip(dhikr: Dhikr, onClick: () -> Unit) {
    val gradients = LocalAppGradients.current
    Row(
        modifier = Modifier
            .clip(Radii.chip)
            .background(Brush.linearGradient(gradients.accent(dhikr.accentIndex)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.l, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = dhikr.displayTitle,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NewCollectionTile(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = Radii.card,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(
                text = "New collection",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Pick a cover, add adhkar, arrange it your way",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
