package com.lillah.dhikr.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.data.prefs.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds only what the whole app needs before the first frame: the theme. Kept separate from screen
 * state so a palette change never re-runs a screen's data flows.
 */
class AppViewModel(container: AppContainer) : ViewModel() {

    private val settingsRepository = container.settingsRepository
    private val clock = container.clock

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            settingsRepository.setLastOpenedDay(clock.todayEpochDay())
        }
    }
}
