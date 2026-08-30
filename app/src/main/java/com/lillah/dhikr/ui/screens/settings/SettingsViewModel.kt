package com.lillah.dhikr.ui.screens.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.data.prefs.UserSettings
import com.lillah.dhikr.ui.theme.ThemeMode
import com.lillah.dhikr.ui.theme.ThemePalette
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val lifetimeTotal: Long = 0,
    val adhkarCount: Int = 0,
    val collectionCount: Int = 0,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settingsRepository = container.settingsRepository
    private val statsRepository = container.statsRepository
    private val dhikrRepository = container.dhikrRepository

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        statsRepository.observeLifetimeTotal(),
        dhikrRepository.observeAll(),
        dhikrRepository.observeCollections(),
    ) { settings, lifetime, adhkar, collections ->
        SettingsUiState(settings, lifetime, adhkar.size, collections.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setPalette(palette: ThemePalette) = launch { settingsRepository.setPalette(palette) }
    fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setHaptics(enabled: Boolean) = launch { settingsRepository.setHaptics(enabled) }
    fun setSound(enabled: Boolean) = launch { settingsRepository.setSound(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = launch { settingsRepository.setKeepScreenOn(enabled) }
    fun setVolumeKeys(enabled: Boolean) = launch { settingsRepository.setVolumeKeys(enabled) }
    fun setShowArabic(enabled: Boolean) = launch { settingsRepository.setShowArabic(enabled) }
    fun setShowTransliteration(enabled: Boolean) =
        launch { settingsRepository.setShowTransliteration(enabled) }
    fun setShowMeaning(enabled: Boolean) = launch { settingsRepository.setShowMeaning(enabled) }
    fun setDailyGoal(goal: Int) = launch { settingsRepository.setDailyGoal(goal) }

    /** Adds back any shipped adhkar that were removed. Counting history is never touched. */
    fun restoreDefaults() = launch {
        val added = dhikrRepository.restoreDefaults()
        _messages.emit(
            when (added) {
                0 -> "Everything is already here — nothing to restore."
                1 -> "1 dhikr restored."
                else -> "$added adhkar restored."
            }
        )
    }

    fun clearToday() = launch {
        statsRepository.clearToday()
        _messages.emit("Today's counts cleared.")
    }

    fun clearAllHistory() = launch {
        statsRepository.clearAllHistory()
        container.gamificationRepository.resetAll()
        _messages.emit("All counting history cleared.")
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
