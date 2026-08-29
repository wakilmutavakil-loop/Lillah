package com.lillah.dhikr.core.di

import android.content.Context
import com.lillah.dhikr.core.feedback.Haptics
import com.lillah.dhikr.core.media.CounterSounds
import com.lillah.dhikr.core.media.CoverImageStore
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.core.time.SystemAppClock
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.prefs.SettingsRepository
import com.lillah.dhikr.data.repository.DhikrRepository
import com.lillah.dhikr.data.repository.GamificationRepository
import com.lillah.dhikr.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Hand-rolled dependency container.
 *
 * The app is a single module with one process-wide object graph and no build-variant swapping, so
 * a container of lazies gives the same constructor injection and the same testability as an
 * annotation processor would, without a second processor in the build or generated code to read
 * through. Everything below is constructed once and shared.
 */
class AppContainer(private val context: Context) {

    val clock: AppClock = SystemAppClock()

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }

    private val database: DhikrDatabase by lazy { DhikrDatabase.build(context) }

    val dhikrRepository: DhikrRepository by lazy {
        DhikrRepository(
            dhikrDao = database.dhikrDao(),
            collectionDao = database.collectionDao(),
            countDao = database.countDao(),
            counterDao = database.counterDao(),
            clock = clock,
        )
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(countDao = database.countDao(), clock = clock)
    }

    val gamificationRepository: GamificationRepository by lazy {
        GamificationRepository(
            achievementDao = database.achievementDao(),
            counterDao = database.counterDao(),
            countDao = database.countDao(),
            dhikrRepository = dhikrRepository,
            clock = clock,
        )
    }

    val haptics: Haptics by lazy { Haptics(context) }
    val sounds: CounterSounds by lazy { CounterSounds() }
    val coverImageStore: CoverImageStore by lazy { CoverImageStore(context) }

    /**
     * Volume-key presses forwarded from the activity. The counter subscribes only while the
     * preference is on, so the keys keep their normal behaviour otherwise.
     */
    private val _hardwareKeyCounts = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val hardwareKeyCounts: SharedFlow<Int> = _hardwareKeyCounts

    fun emitHardwareCount(delta: Int) {
        _hardwareKeyCounts.tryEmit(delta)
    }
}
