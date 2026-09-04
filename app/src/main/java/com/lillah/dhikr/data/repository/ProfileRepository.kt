package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.dao.ProfileDao
import com.lillah.dhikr.data.local.entity.ProfileEntity
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.data.prefs.AccountState
import com.lillah.dhikr.domain.sync.AuthUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Who is using the app, and whose data is on screen.
 *
 * All counting lives on the device, partitioned by profile. Profile 1 is the device profile: it
 * holds everything counted before anybody signed in, and it always exists.
 *
 * The first account to sign in **adopts** that profile rather than starting from zero, which is
 * what makes an upgrade invisible — a user who had 25,000 dhikr before accounts existed signs in
 * and still has 25,000. A different account signing in on the same device gets a profile of its
 * own, seeded fresh and completely isolated. Signing out returns to the device profile.
 *
 * Nothing here removes a profile. Signing out forgets a session; it never forgets a person.
 */
class ProfileRepository(
    private val profileDao: ProfileDao,
    private val accountRepository: AccountRepository,
    private val clock: AppClock,
    scope: CoroutineScope,
) {

    val activeProfileId: StateFlow<Long> = accountRepository.state
        .map { it.activeProfileId }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, AccountState.DEVICE_PROFILE_ID)

    val activeProfile: Flow<ProfileEntity?> = activeProfileId
        .flatMapLatest { profileDao.observeById(it) }

    val profiles: Flow<List<ProfileEntity>> = profileDao.observeAll()

    /** Creates the device profile if this is the very first launch after upgrading. */
    suspend fun ensureDeviceProfile() {
        if (profileDao.deviceProfile() != null) return
        val now = clock.nowMillis()
        profileDao.insert(
            ProfileEntity(
                id = AccountState.DEVICE_PROFILE_ID,
                uid = null,
                displayName = "This device",
                createdAt = now,
                lastActiveAt = now,
            )
        )
    }

    /**
     * Resolves the profile for a signed-in account and makes it active.
     *
     * @return the profile id, and whether it was newly created — a new profile needs seeding,
     *   an adopted or returning one already has its content.
     */
    suspend fun onSignedIn(user: AuthUser): ProfileResolution {
        ensureDeviceProfile()
        val now = clock.nowMillis()

        profileDao.getByUid(user.uid)?.let { existing ->
            profileDao.update(
                existing.copy(
                    displayName = user.displayName ?: existing.displayName,
                    email = user.email ?: existing.email,
                    photoUrl = user.photoUrl ?: existing.photoUrl,
                    method = user.method?.id ?: existing.method,
                    lastActiveAt = now,
                )
            )
            accountRepository.setActiveProfile(existing.id)
            return ProfileResolution(existing.id, isNew = false)
        }

        val device = profileDao.deviceProfile()
        if (device != null && device.uid == null) {
            // Nobody has claimed this device yet, so its history is this account's history.
            profileDao.update(
                device.copy(
                    uid = user.uid,
                    displayName = user.displayName ?: device.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    method = user.method?.id,
                    lastActiveAt = now,
                )
            )
            accountRepository.setActiveProfile(device.id)
            return ProfileResolution(device.id, isNew = false)
        }

        val created = profileDao.insert(
            ProfileEntity(
                uid = user.uid,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl,
                method = user.method?.id,
                createdAt = now,
                lastActiveAt = now,
            )
        )
        val id = if (created > 0) created else profileDao.getByUid(user.uid)?.id
            ?: AccountState.DEVICE_PROFILE_ID
        accountRepository.setActiveProfile(id)
        return ProfileResolution(id, isNew = created > 0)
    }

    suspend fun touchActive() {
        profileDao.touch(activeProfileId.value, clock.nowMillis())
    }
}

data class ProfileResolution(val profileId: Long, val isNew: Boolean)
