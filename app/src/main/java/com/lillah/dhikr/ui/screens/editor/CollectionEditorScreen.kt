package com.lillah.dhikr.ui.screens.editor

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lillah.dhikr.domain.model.CoverArt
import com.lillah.dhikr.ui.components.ConfirmSheet
import com.lillah.dhikr.ui.components.CoverArtworkCanvas
import com.lillah.dhikr.ui.components.LabeledField
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.theme.ArabicText
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import java.io.File

@Composable
fun CollectionEditorScreen(
    state: CollectionEditorState,
    onUpdate: ((CollectionEditorState) -> CollectionEditorState) -> Unit,
    onPickCover: (android.net.Uri) -> Unit,
    onClearCover: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onPickCover(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Spacing.screen),
    ) {
        Spacer(Modifier.height(Spacing.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(Spacing.m))
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        // Live cover preview: whatever is chosen here is exactly what the card will show.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(Radii.cover),
        ) {
            val path = state.coverImagePath
            if (!path.isNullOrBlank()) {
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CoverArtworkCanvas(
                    artwork = state.artwork,
                    seed = state.id,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.l),
            ) {
                if (state.arabicName.isNotBlank()) {
                    Text(
                        text = state.arabicName,
                        style = ArabicText.Caption,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                Text(
                    text = state.name.ifBlank { "Your collection" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(Spacing.m))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(
                onClick = { picker.launch(arrayOf("image/*")) },
                shape = Radii.chip,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(if (state.coverImagePath == null) "Choose an image" else "Replace image")
            }
            if (state.coverImagePath != null) {
                TextButton(onClick = onClearCover) { Text("Use artwork") }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionHeader(title = "Artwork", subtitle = "Used when no image is chosen")
        Spacer(Modifier.height(Spacing.m))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            items(CoverArt.entries.size) { index ->
                val art = CoverArt.entries[index]
                val selected = state.artwork == art && state.coverImagePath == null
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 72.dp)
                        .clip(Radii.tile)
                        .then(
                            if (selected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, Radii.tile)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onUpdate { it.copy(artwork = art) } },
                ) {
                    CoverArtworkCanvas(
                        artwork = art,
                        seed = index.toLong(),
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        LabeledField(
            label = "Name",
            value = state.name,
            onValueChange = { value -> onUpdate { it.copy(name = value) } },
            placeholder = "Before sleep",
        )
        Spacer(Modifier.height(Spacing.m))
        LabeledField(
            label = "Arabic name (optional)",
            value = state.arabicName,
            onValueChange = { value -> onUpdate { it.copy(arabicName = value) } },
            textStyle = ArabicText.Caption,
        )
        Spacer(Modifier.height(Spacing.m))
        LabeledField(
            label = "Description (optional)",
            value = state.description,
            onValueChange = { value -> onUpdate { it.copy(description = value) } },
            singleLine = false,
            minLines = 2,
        )

        Spacer(Modifier.height(Spacing.xxl))

        Button(
            onClick = onSave,
            enabled = state.canSave,
            shape = Radii.chip,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (state.isNew) "Create collection" else "Save changes")
        }

        if (!state.isNew && !state.isBuiltIn) {
            Spacer(Modifier.height(Spacing.s))
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.size(Spacing.s))
                Text("Delete collection", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }

    if (confirmDelete) {
        ConfirmSheet(
            title = "Delete this collection?",
            message = "The adhkar inside it are kept, along with everything you have counted. " +
                "They simply stop belonging to a collection.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}
