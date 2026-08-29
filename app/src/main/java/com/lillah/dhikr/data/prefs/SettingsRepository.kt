package com.lillah.dhikr.data.prefs

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lillah.dhikr.ui.theme.ThemeMode
import com.lillah.dhikr.ui.theme.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("dhikr_settings")

@Immutable
data class UserSettings(
    val palette: ThemePalette = ThemePalette.Default,
    val themeMode: ThemeMode = ThemeMode.System,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val dailyGoal: Int = 100,
    val keepScreenOn: Boolean = true,
    val countWithVolumeKeys: Boolean = false,
    val autoAdvanceInCollection: Boolean = true,
    val showArabic: Boolean = true,
    val showTransliteration: Boolean = true,
    val showMeaning: Boolean = true,
    val onboardingComplete: Boolean = false,
    val activeDhikrId: Long = 0,
    val lastOpenedEpochDay: Long = 0,
    val lastSummaryShownEpochDay: Long = 0,
)

/**
 * Preferences live in DataStore rather than the database: they are small, single-valued, and read
 * on the very first frame, so they should not wait on a Room transaction.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val palette = stringPreferencesKey("palette")
        val themeMode = stringPreferencesKey("theme_mode")
        val haptics = booleanPreferencesKey("haptics")
        val sound = booleanPreferencesKey("sound")
        val dailyGoal = intPreferencesKey("daily_goal")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val volumeKeys = booleanPreferencesKey("volume_keys")
        val autoAdvance = booleanPreferencesKey("auto_advance")
        val showArabic = booleanPreferencesKey("show_arabic")
        val showTransliteration = booleanPreferencesKey("show_transliteration")
        val showMeaning = booleanPreferencesKey("show_meaning")
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val activeDhikr = longPreferencesKey("active_dhikr")
        val lastOpened = longPreferencesKey("last_opened_day")
        val lastSummary = longPreferencesKey("last_summary_day")
    }

    val settings: Flow<UserSettings> = context.settingsStore.data
        .catch { error ->
            // A corrupt preferences file must never stop the app from opening.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            val defaults = UserSettings()
            UserSettings(
                palette = ThemePalette.fromKey(prefs[Keys.palette]),
                themeMode = ThemeMode.fromKey(prefs[Keys.themeMode]),
                hapticsEnabled = prefs[Keys.haptics] ?: defaults.hapticsEnabled,
                soundEnabled = prefs[Keys.sound] ?: defaults.soundEnabled,
                dailyGoal = prefs[Keys.dailyGoal] ?: defaults.dailyGoal,
                keepScreenOn = prefs[Keys.keepScreenOn] ?: defaults.keepScreenOn,
                countWithVolumeKeys = prefs[Keys.volumeKeys] ?: defaults.countWithVolumeKeys,
                autoAdvanceInCollection = prefs[Keys.autoAdvance] ?: defaults.autoAdvanceInCollection,
                showArabic = prefs[Keys.showArabic] ?: defaults.showArabic,
                showTransliteration = prefs[Keys.showTransliteration] ?: defaults.showTransliteration,
                showMeaning = prefs[Keys.showMeaning] ?: defaults.showMeaning,
                onboardingComplete = prefs[Keys.onboarding] ?: defaults.onboardingComplete,
                activeDhikrId = prefs[Keys.activeDhikr] ?: defaults.activeDhikrId,
                lastOpenedEpochDay = prefs[Keys.lastOpened] ?: defaults.lastOpenedEpochDay,
                lastSummaryShownEpochDay = prefs[Keys.lastSummary] ?: defaults.lastSummaryShownEpochDay,
            )
        }

    suspend fun setPalette(palette: ThemePalette) = edit { it[Keys.palette] = palette.key }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.key }
    suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.haptics] = enabled }
    suspend fun setSound(enabled: Boolean) = edit { it[Keys.sound] = enabled }
    suspend fun setDailyGoal(goal: Int) = edit { it[Keys.dailyGoal] = goal.coerceIn(10, 10_000) }
    suspend fun setKeepScreenOn(enabled: Boolean) = edit { it[Keys.keepScreenOn] = enabled }
    suspend fun setVolumeKeys(enabled: Boolean) = edit { it[Keys.volumeKeys] = enabled }
    suspend fun setAutoAdvance(enabled: Boolean) = edit { it[Keys.autoAdvance] = enabled }
    suspend fun setShowArabic(enabled: Boolean) = edit { it[Keys.showArabic] = enabled }
    suspend fun setShowTransliteration(enabled: Boolean) = edit { it[Keys.showTransliteration] = enabled }
    suspend fun setShowMeaning(enabled: Boolean) = edit { it[Keys.showMeaning] = enabled }
    suspend fun setOnboardingComplete(done: Boolean) = edit { it[Keys.onboarding] = done }
    suspend fun setActiveDhikr(id: Long) = edit { it[Keys.activeDhikr] = id }
    suspend fun setLastOpenedDay(day: Long) = edit { it[Keys.lastOpened] = day }
    suspend fun setLastSummaryShown(day: Long) = edit { it[Keys.lastSummary] = day }

    suspend fun resetToDefaults() {
        context.settingsStore.edit { it.clear() }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit(block)
    }
}
