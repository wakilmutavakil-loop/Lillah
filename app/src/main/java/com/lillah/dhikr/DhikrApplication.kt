package com.lillah.dhikr

import android.app.Application
import com.lillah.dhikr.core.di.AppContainer
import kotlinx.coroutines.launch

class DhikrApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seeding is idempotent and cheap once the content is in place, so it runs on every
        // start rather than from a Room create-callback: no re-entrancy into an open transaction,
        // and content the user has cleared out is never silently resurrected mid-session.
        // Drains the outbox in the background whenever there is something queued and an account
        // to attach it to. A no-op in a build with no backend configured.
        container.syncRepository.start(container.applicationScope)

        container.applicationScope.launch {
            // The device profile must exist before anything reads or writes profile-scoped data.
            container.profileRepository.ensureDeviceProfile()
            container.dhikrRepository.seedIfEmpty()
        }
    }

    override fun onTerminate() {
        container.sounds.release()
        super.onTerminate()
    }
}
