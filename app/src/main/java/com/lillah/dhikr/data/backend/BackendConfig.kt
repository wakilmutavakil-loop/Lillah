package com.lillah.dhikr.data.backend

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.lillah.dhikr.BuildConfig

/** Backend credentials supplied at build time from a gitignored `backend.properties`. */
object BackendConfig {
    val firebaseApiKey: String get() = BuildConfig.FIREBASE_API_KEY
    val firebaseAppId: String get() = BuildConfig.FIREBASE_APP_ID
    val firebaseProjectId: String get() = BuildConfig.FIREBASE_PROJECT_ID
    val firebaseSenderId: String get() = BuildConfig.FIREBASE_SENDER_ID
    val googleWebClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val facebookAppId: String get() = BuildConfig.FACEBOOK_APP_ID
    val facebookClientToken: String get() = BuildConfig.FACEBOOK_CLIENT_TOKEN

    /** The minimum Firebase needs to reach a project. */
    val hasFirebase: Boolean
        get() = firebaseApiKey.isNotBlank() &&
            firebaseAppId.isNotBlank() &&
            firebaseProjectId.isNotBlank()

    val hasGoogleSignIn: Boolean get() = hasFirebase && googleWebClientId.isNotBlank()

    val hasFacebookSignIn: Boolean
        get() = hasFirebase && facebookAppId.isNotBlank() && facebookClientToken.isNotBlank()
}

/**
 * Brings Firebase up from [BackendConfig] instead of google-services.json.
 *
 * Idempotent and safe to call from anywhere; the result is cached because it cannot change during
 * a process's life.
 */
object FirebaseInitializer {

    @Volatile
    private var resolved: Boolean? = null

    fun ensureInitialized(context: Context): Boolean {
        resolved?.let { return it }
        synchronized(this) {
            resolved?.let { return it }
            val outcome = runCatching {
                if (!BackendConfig.hasFirebase) return@runCatching false
                val app = context.applicationContext
                if (FirebaseApp.getApps(app).isNotEmpty()) return@runCatching true

                val options = FirebaseOptions.Builder()
                    .setApiKey(BackendConfig.firebaseApiKey)
                    .setApplicationId(BackendConfig.firebaseAppId)
                    .setProjectId(BackendConfig.firebaseProjectId)
                    .apply {
                        if (BackendConfig.firebaseSenderId.isNotBlank()) {
                            setGcmSenderId(BackendConfig.firebaseSenderId)
                        }
                    }
                    .build()
                FirebaseApp.initializeApp(app, options)
                true
            }.getOrDefault(false)
            resolved = outcome
            return outcome
        }
    }
}
