package com.lillah.dhikr.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.core.time.grouped
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.components.SwitchRow
import com.lillah.dhikr.ui.components.TargetSheet
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import com.lillah.dhikr.ui.theme.ThemeMode
import com.lillah.dhikr.ui.theme.ThemePalette

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    isDark: Boolean,
    onSelectPalette: (ThemePalette) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleVolumeKeys: (Boolean) -> Unit,
    onToggleArabic: (Boolean) -> Unit,
    onToggleTransliteration: (Boolean) -> Unit,
    onToggleMeaning: (Boolean) -> Unit,
    onSetDailyGoal: (Int) -> Unit,
    onManageAdhkar: () -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var goalSheet by remember { mutableStateOf(false) }
    val settings = state.settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Spacer(Modifier.height(Spacing.s))
        SectionHeader(
            title = "Settings",
            subtitle = "${state.lifetimeTotal.grouped()} remembrances so far",
            modifier = Modifier.padding(horizontal = Spacing.screen),
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionHeader(
            title = "Theme",
            subtitle = "Tap one to see the whole app change",
            modifier = Modifier.padding(horizontal = Spacing.screen),
        )
        Spacer(Modifier.height(Spacing.m))
        ThemeGallery(
            selected = settings.palette,
            dark = isDark,
            onSelect = onSelectPalette,
        )

        Spacer(Modifier.height(Spacing.l))
        Box(Modifier.padding(horizontal = Spacing.screen)) {
            ThemeModeSelector(selected = settings.themeMode, onSelect = onSelectMode)
        }

        Spacer(Modifier.height(Spacing.xl))
        Group(title = "Your goal") {
            ValueRow(
                title = "Daily goal",
                description = "The floor for a normal day, not the ceiling",
                value = settings.dailyGoal.grouped(),
                onClick = { goalSheet = true },
            )
        }

        Spacer(Modifier.height(Spacing.l))
        Group(title = "Counting") {
            SwitchRow(
                title = "Haptic feedback",
                description = "A short pulse on every count, longer when a round completes",
                checked = settings.hapticsEnabled,
                onCheckedChange = onToggleHaptics,
            )
            Spacer(Modifier.height(Spacing.l))
            SwitchRow(
                title = "Counting sound",
                description = "A soft click, and a chime at the end of a round",
                checked = settings.soundEnabled,
                onCheckedChange = onToggleSound,
            )
            Spacer(Modifier.height(Spacing.l))
            SwitchRow(
                title = "Count with volume keys",
                description = "Volume up counts, volume down undoes — useful with the screen off",
                checked = settings.countWithVolumeKeys,
                onCheckedChange = onToggleVolumeKeys,
            )
            Spacer(Modifier.height(Spacing.l))
            SwitchRow(
                title = "Keep the screen on",
                description = "While the app is open, so a long sitting is not interrupted",
                checked = settings.keepScreenOn,
                onCheckedChange = onToggleKeepScreenOn,
            )
        }

        Spacer(Modifier.height(Spacing.l))
        Group(title = "What to show") {
            SwitchRow(
                title = "Arabic text",
                checked = settings.showArabic,
                onCheckedChange = onToggleArabic,
            )
            Spacer(Modifier.height(Spacing.l))
            SwitchRow(
                title = "Transliteration",
                checked = settings.showTransliteration,
                onCheckedChange = onToggleTransliteration,
            )
            Spacer(Modifier.height(Spacing.l))
            SwitchRow(
                title = "English meaning",
                checked = settings.showMeaning,
                onCheckedChange = onToggleMeaning,
            )
        }

        Spacer(Modifier.height(Spacing.l))
        Group(title = "Your adhkar") {
            ValueRow(
                title = "Manage adhkar",
                description = "${state.adhkarCount} adhkar in ${state.collectionCount} collections",
                value = "",
                onClick = onManageAdhkar,
            )
            Spacer(Modifier.height(Spacing.l))
            ValueRow(
                title = "Restore the default adhkar",
                description = "Adds back anything shipped that you removed. " +
                    "Your counts and edits are untouched.",
                value = "",
                onClick = onRestoreDefaults,
            )
        }

        Spacer(Modifier.height(Spacing.l))
        Group(title = "Your data") {
            Text(
                text = "Everything you count is stored on this device and nowhere else. Only a " +
                    "running total is ever sent, and only to add your contribution to the " +
                    "worldwide count.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = "Nothing you have counted can be deleted from inside the app. There is no " +
                    "reset, no clear history, and no way to remove a dhikr — only to archive it, " +
                    "which keeps everything and simply puts it out of the way.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.l))
        Box(Modifier.padding(horizontal = Spacing.screen)) {
            SoftCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "Dhikr — ذِكْر",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Version 1.0.0 · works offline · no account, no tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.m))
                Text(
                    text = "Adhkar are reproduced from widely transmitted sources and cited " +
                        "where a well-known narration exists. This app is a counter, not a " +
                        "scholarly reference — check anything you are unsure of, and edit any " +
                        "wording to match the reading you follow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
    }

    if (goalSheet) {
        TargetSheet(
            current = settings.dailyGoal,
            title = "Daily goal",
            presets = listOf(33, 50, 100, 200, 300, 500, 1000),
            onDismiss = { goalSheet = false },
            onConfirm = {
                onSetDailyGoal(it)
                goalSheet = false
            },
        )
    }
}

@Composable
private fun Group(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.screen)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.s),
        )
        SoftCard(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    description: String? = null,
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
