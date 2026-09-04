package com.lillah.dhikr.core.di

import android.content.Context
import com.lillah.dhikr.core.feedback.Haptics
import com.lillah.dhikr.core.media.CounterSounds
import com.lillah.dhikr.core.media.CoverImageStore
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.core.time.SystemAppClock
import com.lillah.dhikr.data.backend.AuthGateway
import com.lillah.dhikr.data.backend.BackendFactory
import com.lillah.dhikr.data.backend.DhikrBackend
import com.lillah.dhikr.data.backend.UnconfiguredAuthGateway
import com.lillah.dhikr.data.backend.UnconfiguredBackend
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.data.prefs.SettingsRepository
import com.lillah.dhikr.data.repository.DhikrRepository
import com.lillah.dhikr.data.repository.GamificationRepository
import com.lillah.dhikr.data.repository.ProfileRepository
import com.lillah.dhikr.data.repository.StatsRepository
import com.lillah.dhikr.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    /**
     * Process-lifetime scope. Owned here rather than in the Application so repositories that need
     * to hold state across the app's life can be constructed with it.
     */
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }

    val accountRepository: AccountRepository by lazy { AccountRepository(context) }

    /**
     * Resolved once at startup. A build without backend credentials gets the unconfigured pair,
     * which behaves like a permanently offline device — a state the sync engine already handles,
     * so nothing above this line needs to know the difference.
     */
    val backend: DhikrBackend by lazy { BackendFactory.createBackend(context) }

    val authGateway: AuthGateway by lazy { BackendFactory.createAuthGateway(context) }

    private val database: DhikrDatabase by lazy { DhikrDatabase.build(context) }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            profileDao = database.profileDao(),
            accountRepository = accountRepository,
            clock = clock,
            scope = applicationScope,
        )
    }

    val dhikrRepository: DhikrRepository by lazy {
        DhikrRepository(
            database = database,
            dhikrDao = database.dhikrDao(),
            collectionDao = database.collectionDao(),
            countDao = database.countDao(),
            counterDao = database.counterDao(),
            syncDao = database.syncDao(),
            profiles = profileRepository,
            clock = clock,
        )
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(
            countDao = database.countDao(),
            profiles = profileRepository,
            clock = clock,
        )
    }

    val gamificationRepository: GamificationRepository by lazy {
        GamificationRepository(
            achievementDao = database.achievementDao(),
            counterDao = database.counterDao(),
            countDao = database.countDao(),
            dhikrRepository = dhikrRepository,
            profiles = profileRepository,
            clock = clock,
        )
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(
            syncDao = database.syncDao(),
            countDao = database.countDao(),
            accountRepository = accountRepository,
            profiles = profileRepository,
            backend = backend,
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
