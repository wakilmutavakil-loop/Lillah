package com.lillah.dhikr.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.domain.model.DhikrCollection
import com.lillah.dhikr.ui.components.ChoicePill
import com.lillah.dhikr.ui.components.ConfirmSheet
import com.lillah.dhikr.ui.components.LabeledField
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.components.SwitchRow
import com.lillah.dhikr.ui.components.TargetSheet
import com.lillah.dhikr.ui.theme.ArabicText
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing

@Composable
fun DhikrEditorScreen(
    state: DhikrEditorState,
    collections: List<DhikrCollection>,
    onUpdate: ((DhikrEditorState) -> DhikrEditorState) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var targetSheet by remember { mutableStateOf(false) }
    var dailySheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Spacing.screen),
    ) {
        Spacer(Modifier.height(Spacing.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.size(Spacing.m))
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        if (!state.arabic.isBlank()) {
            SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
                Text(
                    text = state.arabic,
                    style = ArabicText.Body,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Spacing.l))
        }

        LabeledField(
            label = "Name",
            value = state.name,
            onValueChange = { value -> onUpdate { it.copy(name = value) } },
            placeholder = "SubhanAllah",
        )
        Spacer(Modifier.height(Spacing.m))

        LabeledField(
            label = "Arabic (optional)",
            value = state.arabic,
            onValueChange = { value -> onUpdate { it.copy(arabic = value) } },
            singleLine = false,
            minLines = 2,
            textStyle = ArabicText.Caption,
        )
        Spacer(Modifier.height(Spacing.m))

        LabeledField(
            label = "Transliteration (optional)",
            value = state.transliteration,
            onValueChange = { value -> onUpdate { it.copy(transliteration = value) } },
        )
        Spacer(Modifier.height(Spacing.m))

        LabeledField(
            label = "Meaning (optional)",
            value = state.meaning,
            onValueChange = { value -> onUpdate { it.copy(meaning = value) } },
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(Spacing.m))

        LabeledField(
            label = "Note or virtue (optional)",
            value = state.virtue,
            onValueChange = { value -> onUpdate { it.copy(virtue = value) } },
            singleLine = false,
        )
        Spacer(Modifier.height(Spacing.m))

        LabeledField(
            label = "Source (optional)",
            value = state.source,
            onValueChange = { value -> onUpdate { it.copy(source = value) } },
            placeholder = "Sahih al-Bukhari",
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionHeader(title = "Counts")
        Spacer(Modifier.height(Spacing.m))

        SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
            ValueRow(
                title = "Repetitions per round",
                value = state.targetCount.toString(),
                onClick = { targetSheet = true },
            )
            Spacer(Modifier.height(Spacing.l))
            ValueRow(
                title = "Daily goal for this dhikr",
                value = state.dailyTarget?.toString() ?: "Not set",
                onClick = { dailySheet = true },
                onClear = if (state.dailyTarget != null) {
                    { onUpdate { it.copy(dailyTarget = null) } }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(Spacing.l))
            SwitchRow(
                title = "Favourite",
                description = "Show it on the Adhkar screen for quick access",
                checked = state.isFavorite,
                onCheckedChange = { value -> onUpdate { it.copy(isFavorite = value) } },
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionHeader(title = "Collection")
        Spacer(Modifier.height(Spacing.m))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            ChoicePill(
                label = "None",
                selected = state.collectionId == null,
                onClick = { onUpdate { it.copy(collectionId = null) } },
            )
            collections.forEach { collection ->
                ChoicePill(
                    label = collection.name,
                    selected = state.collectionId == collection.id,
                    onClick = { onUpdate { it.copy(collectionId = collection.id) } },
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionHeader(title = "Colour")
        Spacer(Modifier.height(Spacing.m))
        AccentPicker(
            selected = state.accentIndex,
            onSelect = { index -> onUpdate { it.copy(accentIndex = index) } },
        )

        Spacer(Modifier.height(Spacing.xxl))

        Button(
            onClick = onSave,
            enabled = state.canSave,
            shape = Radii.chip,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (state.isNew) "Add dhikr" else "Save changes")
        }

        if (!state.isNew) {
            Spacer(Modifier.height(Spacing.s))
            TextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Rounded.Inventory2,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(Spacing.s))
                Text(
                    "Archive — keeps its history",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                Text("Remove this dhikr", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }

    if (targetSheet) {
        TargetSheet(
            current = state.targetCount,
            onDismiss = { targetSheet = false },
            onConfirm = { value ->
                onUpdate { it.copy(targetCount = value) }
                targetSheet = false
            },
        )
    }

    if (dailySheet) {
        TargetSheet(
            current = state.dailyTarget ?: state.targetCount,
            title = "Daily goal for this dhikr",
            presets = listOf(10, 33, 66, 100, 200, 300, 500, 1000),
            onDismiss = { dailySheet = false },
            onConfirm = { value ->
                onUpdate { it.copy(dailyTarget = value) }
                dailySheet = false
            },
        )
    }

    if (confirmDelete) {
        ConfirmSheet(
            title = "Remove this dhikr?",
            message = "Its counting history goes with it. If you only want it out of the way, " +
                "archive it instead — the history stays and you can bring it back later.",
            confirmLabel = "Remove",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}

@Composable
private fun ValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.field)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onClear != null) {
            Spacer(Modifier.size(Spacing.s))
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

@Composable
private fun AccentPicker(selected: Int, onSelect: (Int) -> Unit) {
    val gradients = LocalAppGradients.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        gradients.accents.forEachIndexed { index, colors ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors))
                    .then(
                        if (selected == index) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == index) {
                    Icon(
                        Icons.Rounded.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
