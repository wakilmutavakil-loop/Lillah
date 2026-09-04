package com.lillah.dhikr.data.prefs

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lillah.dhikr.domain.sync.AuthMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.accountStore: DataStore<Preferences> by preferencesDataStore("dhikr_account")

@Immutable
data class AccountState(
    /** Stable per-installation id. Scopes the one-time baseline claim to this device. */
    val deviceId: String = "",
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val method: AuthMethod? = null,
    val lastSyncAt: Long? = null,
    val lastSyncError: String? = null,
    /** Which local profile's data the app is showing. 1 is the device profile. */
    val activeProfileId: Long = DEVICE_PROFILE_ID,
    val lastConnectPromptAt: Long = 0,
) {
    val isSignedIn: Boolean get() = !uid.isNullOrBlank()

    companion object {
        const val DEVICE_PROFILE_ID = 1L
    }
}

/**
 * Who is signed in, and what this device has already contributed under that account.
 *
 * Kept in its own DataStore file rather than alongside display preferences so that clearing
 * settings can never take an account, and so a corrupt preferences file on one side leaves the
 * other readable.
 */
class AccountRepository(private val context: Context) {

    private object Keys {
        val deviceId = stringPreferencesKey("device_id")
        val uid = stringPreferencesKey("uid")
        val displayName = stringPreferencesKey("display_name")
        val email = stringPreferencesKey("email")
        val photoUrl = stringPreferencesKey("photo_url")
        val method = stringPreferencesKey("auth_method")
        val lastSyncAt = longPreferencesKey("last_sync_at")
        val lastSyncError = stringPreferencesKey("last_sync_error")
        val activeProfileId = longPreferencesKey("active_profile_id")
        val lastConnectPromptAt = longPreferencesKey("last_connect_prompt_at")
    }

    val state: Flow<AccountState> = context.accountStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            AccountState(
                deviceId = prefs[Keys.deviceId].orEmpty(),
                uid = prefs[Keys.uid],
                displayName = prefs[Keys.displayName],
                email = prefs[Keys.email],
                photoUrl = prefs[Keys.photoUrl],
                method = AuthMethod.entries.firstOrNull { it.id == prefs[Keys.method] },
                lastSyncAt = prefs[Keys.lastSyncAt],
                lastSyncError = prefs[Keys.lastSyncError],
                activeProfileId = prefs[Keys.activeProfileId] ?: AccountState.DEVICE_PROFILE_ID,
                lastConnectPromptAt = prefs[Keys.lastConnectPromptAt] ?: 0,
            )
        }

    /**
     * Returns this installation's id, generating and persisting one on first use.
     *
     * Not a hardware identifier: those are restricted, and a random per-install value is exactly
     * what the baseline claim needs — reinstalling should look like a new device, because its
     * local history is genuinely new to the account.
     */
    suspend fun deviceId(): String {
        val existing = state.first().deviceId
        if (existing.isNotBlank()) return existing
        val generated = UUID.randomUUID().toString()
        context.accountStore.edit { it[Keys.deviceId] = generated }
        return generated
    }

    suspend fun setSignedIn(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?,
        method: AuthMethod?,
    ) {
        context.accountStore.edit { prefs ->
            prefs[Keys.uid] = uid
            displayName?.let { prefs[Keys.displayName] = it } ?: prefs.remove(Keys.displayName)
            email?.let { prefs[Keys.email] = it } ?: prefs.remove(Keys.email)
            photoUrl?.let { prefs[Keys.photoUrl] = it } ?: prefs.remove(Keys.photoUrl)
            method?.let { prefs[Keys.method] = it.id } ?: prefs.remove(Keys.method)
            prefs.remove(Keys.lastSyncError)
        }
    }

    /**
     * Forgets the account only. No counting history, queued operation or preference is touched —
     * signing out is not a request to erase anything, and a user who signs back in must find
     * everything where they left it.
     */
    suspend fun clearSignedIn() {
        context.accountStore.edit { prefs ->
            prefs[Keys.activeProfileId] = AccountState.DEVICE_PROFILE_ID
            prefs.remove(Keys.uid)
            prefs.remove(Keys.displayName)
            prefs.remove(Keys.email)
            prefs.remove(Keys.photoUrl)
            prefs.remove(Keys.method)
            prefs.remove(Keys.lastSyncError)
        }
    }

    suspend fun setActiveProfile(profileId: Long) {
        context.accountStore.edit { it[Keys.activeProfileId] = profileId }
    }

    suspend fun recordConnectPrompt(at: Long) {
        context.accountStore.edit { it[Keys.lastConnectPromptAt] = at }
    }

    suspend fun recordSyncSuccess(at: Long) {
        context.accountStore.edit { prefs ->
            prefs[Keys.lastSyncAt] = at
            prefs.remove(Keys.lastSyncError)
        }
    }

    suspend fun recordSyncFailure(message: String?) {
        context.accountStore.edit { prefs ->
            if (message.isNullOrBlank()) prefs.remove(Keys.lastSyncError)
            else prefs[Keys.lastSyncError] = message
        }
    }
}
