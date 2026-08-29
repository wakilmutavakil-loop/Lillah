package com.lillah.dhikr

import android.app.Application
import com.lillah.dhikr.core.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DhikrApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seeding is idempotent and cheap once the content is in place, so it runs on every
        // start rather than from a Room create-callback: no re-entrancy into an open transaction,
        // and content the user has cleared out is never silently resurrected mid-session.
        applicationScope.launch {
            container.dhikrRepository.seedIfEmpty()
        }
    }

    override fun onTerminate() {
        container.sounds.release()
        super.onTerminate()
    }
}
