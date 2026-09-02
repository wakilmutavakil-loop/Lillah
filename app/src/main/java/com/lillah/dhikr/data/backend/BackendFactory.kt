package com.lillah.dhikr.data.backend

import android.content.Context

/**
 * Decides at runtime whether this build has a cloud.
 *
 * Firebase is initialised programmatically from [BackendConfig] rather than through the
 * google-services Gradle plugin, deliberately: the plugin fails the build outright when
 * google-services.json is absent, which would mean nobody could compile the app without first
 * being handed somebody's project credentials. Here, a build with no credentials compiles, runs,
 * and counts dhikr exactly as before — it simply has no cloud attached.
 */
object BackendFactory {

    fun createBackend(context: Context): DhikrBackend =
        if (FirebaseInitializer.ensureInitialized(context)) {
            FirestoreBackend()
        } else {
            UnconfiguredBackend()
        }

    fun createAuthGateway(context: Context): AuthGateway =
        if (FirebaseInitializer.ensureInitialized(context)) {
            FirebaseAuthGateway(context)
        } else {
            UnconfiguredAuthGateway()
        }
}
