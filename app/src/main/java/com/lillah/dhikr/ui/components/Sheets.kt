package com.lillah.dhikr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing

/**
 * Target picker. The presets are the counts people actually use — the tasbih thirds, the hundreds,
 * the sevens — with a stepper for anything else, so the common case is one tap.
 */
@Composable
fun TargetSheet(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    title: String = "Repetitions per round",
    presets: List<Int> = listOf(1, 3, 7, 10, 33, 34, 66, 99, 100, 300, 500, 1000),
) {
    var value by remember { mutableIntStateOf(current.coerceAtLeast(1)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Radii.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                StepperButton(Icons.Rounded.Remove, "Decrease") {
                    value = (value - stepFor(value)).coerceAtLeast(1)
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(140.dp),
                )
                StepperButton(Icons.Rounded.Add, "Increase") {
                    value = (value + stepFor(value)).coerceAtMost(10_000)
                }
            }

            Spacer(Modifier.height(Spacing.l))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                presets.forEach { preset ->
                    ChoicePill(
                        label = preset.toString(),
                        selected = value == preset,
                        onClick = { value = preset },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onConfirm(value) },
                    modifier = Modifier.weight(1f),
                    shape = Radii.chip,
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/** Steps grow with the number so getting from 33 to 500 does not take four hundred taps. */
private fun stepFor(value: Int): Int = when {
    value < 20 -> 1
    value < 100 -> 5
    value < 500 -> 25
    else -> 100
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
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
        Icon(icon, description, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Two-button confirmation used before anything destructive. */
@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Radii.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Keep it")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = Radii.chip,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (destructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}
