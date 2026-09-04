package com.lillah.dhikr.ui.screens.collections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lillah.dhikr.domain.model.DhikrProgress
import com.lillah.dhikr.ui.components.CoverArtworkCanvas
import com.lillah.dhikr.ui.components.ProgressRing
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.theme.ArabicText
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import java.io.File

@Composable
fun CollectionDetailScreen(
    state: CollectionDetailUiState,
    onBack: () -> Unit,
    onOpenDhikr: (Long) -> Unit,
    onCountOne: (Long) -> Unit,
    onUndoOne: (Long) -> Unit,
    onPickCover: (android.net.Uri) -> Unit,
    onClearCover: () -> Unit,
    onEditCollection: () -> Unit,
    onAddDhikr: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // OpenDocument rather than the photo picker: the picked image is copied into app storage
    // straight away, and this contract works the same on every supported Android version.
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onPickCover(uri) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            CollectionHeader(
                state = state,
                onBack = onBack,
                onChangeCover = { picker.launch(arrayOf("image/*")) },
                onClearCover = onClearCover,
                onEdit = onEditCollection,
            )
        }

        item {
            Spacer(Modifier.height(Spacing.l))
            if (!state.collection?.description.isNullOrBlank()) {
                Text(
                    text = state.collection?.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.screen),
                )
                Spacer(Modifier.height(Spacing.l))
            }
        }

        itemsIndexed(
            items = state.items,
            key = { _, item -> item.dhikr.id },
        ) { index, item ->
            Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)) {
                DhikrRow(
                    index = index + 1,
                    item = item,
                    onOpen = { onOpenDhikr(item.dhikr.id) },
                    onCount = { onCountOne(item.dhikr.id) },
                    onUndo = { onUndoOne(item.dhikr.id) },
                )
            }
        }

        item {
            Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.m)) {
                AddDhikrRow(onClick = onAddDhikr)
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun CollectionHeader(
    state: CollectionDetailUiState,
    onBack: () -> Unit,
    onChangeCover: () -> Unit,
    onClearCover: () -> Unit,
    onEdit: () -> Unit,
) {
    val collection = state.collection
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)),
    ) {
        val coverPath = collection?.coverImagePath
        if (!coverPath.isNullOrBlank()) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (collection != null) {
            CoverArtworkCanvas(
                artwork = collection.artwork,
                seed = collection.id,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.28f),
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.68f),
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            GlassIconButton(Icons.Rounded.Image, "Change cover", onChangeCover)
            Spacer(Modifier.size(Spacing.s))
            Box {
                GlassIconButton(Icons.Rounded.MoreHoriz, "More") { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit collection") },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    if (!collection?.coverImagePath.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("Use the default artwork") },
                            onClick = { menuOpen = false; onClearCover() },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(Spacing.screen),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                if (!collection?.arabicName.isNullOrBlank()) {
                    Text(
                        text = collection?.arabicName.orEmpty(),
                        style = ArabicText.Caption,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
                Text(
                    text = collection?.name.orEmpty(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.isComplete) {
                        "Complete for today"
                    } else {
                        "${state.completed} of ${state.items.size} done today"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }

            ProgressRing(
                progress = state.fraction,
                diameter = 66.dp,
                strokeWidth = 7.dp,
                colors = listOf(Color.White, Color.White),
                trackColor = Color.White.copy(alpha = 0.3f),
            ) {
                Text(
                    text = "${(state.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun DhikrRow(
    index: Int,
    item: DhikrProgress,
    onOpen: () -> Unit,
    onCount: () -> Unit,
    onUndo: () -> Unit,
) {
    val gradients = LocalAppGradients.current
    val accent = gradients.accent(item.dhikr.accentIndex)
    val done = item.isCompleteToday

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = Spacing.l,
        elevation = if (done) 4.dp else 8.dp,
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .then(
                        if (done) {
                            Modifier.background(Brush.linearGradient(accent))
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        Icons.Rounded.Check,
                        "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(Spacing.m))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.dhikr.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.dhikr.transliteration.isNullOrBlank()) {
                    Text(
                        text = item.dhikr.transliteration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.size(Spacing.s))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.countToday} / ${item.goal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(Modifier.size(Spacing.s))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(accent))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCount,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    "Count one",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        if (!item.dhikr.arabic.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = item.dhikr.arabic,
                style = ArabicText.Body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }

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
                    .fillMaxWidth(item.fraction)
                    .height(5.dp)
                    .clip(Radii.chip)
                    .background(Brush.horizontalGradient(accent))
            )
        }
    }
}

@Composable
private fun AddDhikrRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, Radii.card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = "Add a dhikr to this collection",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
