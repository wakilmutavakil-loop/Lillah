package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.backend.BackendUnavailable
import com.lillah.dhikr.data.backend.DhikrBackend
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.SyncDao
import com.lillah.dhikr.data.local.entity.RemoteSnapshotEntity
import com.lillah.dhikr.data.local.entity.ProfileSyncStateEntity
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.domain.sync.RemoteFigures
import com.lillah.dhikr.domain.sync.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object NotSignedIn : Exception("Sign in to add your dhikr to the world count.")

/**
 * Adds this device's contribution to the worldwide figure, and reads that figure back.
 *
 * The design is deliberately small. Counting is local and already safe; the network exists only to
 * publish one number and fetch another. So a connect is:
 *
 *  1. read the lifetime total from the device,
 *  2. write it to the cloud as an absolute figure,
 *  3. remember what was accepted,
 *  4. read the worldwide total back.
 *
 * **One document write, however much was counted in between.** An earlier version uploaded a
 * document per tap, which cost a hundred writes for a hundred dhikr and would have exhausted a
 * free Firestore quota at about fifty active people. This costs the same whether somebody counted
 * three or three thousand.
 *
 * **Retrying cannot double count**, because nothing is being added — the same total written twice
 * leaves the cloud where it already was. There is no idempotency key to get wrong.
 *
 * **Nothing is ever lost by a failure.** What is still owed to the world count is not a queue that
 * could be dropped; it is the arithmetic difference between the device's lifetime total and the
 * figure the cloud last accepted. A device that has never connected therefore reports its entire
 * history as waiting, with no bookkeeping required to discover that.
 */
class SyncRepository(
    private val syncDao: SyncDao,
    private val countDao: CountDao,
    private val accountRepository: AccountRepository,
    private val profiles: ProfileRepository,
    private val backend: DhikrBackend,
    private val clock: AppClock,
) {

    private val profileId: Long get() = profiles.activeProfileId.value

    private val syncMutex = Mutex()
    private val syncing = MutableStateFlow(false)

    /** Counted here but not yet reflected in the worldwide figure. */
    private val pendingTotal: Flow<Long> = profiles.activeProfileId.flatMapLatest { pid ->
        combine(
            countDao.observeLifetimeTotal(pid),
            syncDao.observeSyncState(pid),
        ) { lifetime, state ->
            (lifetime - (state?.lastUploadedTotal ?: 0L)).coerceAtLeast(0L)
        }
    }

    val status: Flow<SyncStatus> = combine(
        accountRepository.state,
        pendingTotal,
        syncing,
    ) { account, pending, isSyncing ->
        SyncStatus(
            backendConfigured = backend.isConfigured,
            signedIn = account.isSignedIn,
            syncing = isSyncing,
            // One connect settles everything, so "how many operations" is always one or none.
            pendingOperations = if (pending > 0) 1 else 0,
            pendingTotal = pending,
            lastSyncAt = account.lastSyncAt,
            lastError = account.lastSyncError,
        )
    }

    /** Last known worldwide figures, served from cache so the board renders offline. */
    val cachedFigures: Flow<RemoteFigures?> = syncDao.observeSnapshot().map { snapshot ->
        snapshot?.let {
            RemoteFigures(
                globalTotal = it.globalTotal,
                globalToday = it.globalToday,
                participantCount = it.participantCount,
                userTotal = it.userTotal,
                updatedAt = it.updatedAt,
            )
        }
    }

    /**
     * Publishes this device's total and reads the worldwide figure back.
     *
     * Safe to call as often as you like: a concurrent call returns immediately, and a repeated
     * one writes the same number again to no effect.
     */
    suspend fun syncNow(): Result<Unit> {
        if (!backend.isConfigured) return Result.failure(BackendUnavailable)
        val account = accountRepository.state.first()
        val uid = account.uid ?: return Result.failure(NotSignedIn)
        if (syncMutex.isLocked) return Result.success(Unit)

        return syncMutex.withLock {
            syncing.value = true
            try {
                val pid = profileId
                val today = clock.todayEpochDay()
                val now = clock.nowMillis()
                val total = countDao.lifetimeTotal(pid)
                val todayTotal = countDao.dayTotal(pid, today).toLong()

                backend.publishContribution(uid, total, todayTotal, today)
                    .onFailure { error ->
                        // The local record is untouched, and what is owed is still simply
                        // (lifetime - lastUploaded). Nothing to recover, nothing to retry from.
                        syncDao.markAttemptFailed(pid, error.userMessage(), now)
                        accountRepository.recordSyncFailure(error.userMessage())
                        return@withLock Result.failure(error)
                    }

                syncDao.putSyncState(ProfileSyncStateEntity(pid, total, now))
                syncDao.markAllSynced(pid, uid, now)
                accountRepository.recordSyncSuccess(now)

                backend.fetchFigures(today)
                    .onSuccess { cache(it) }
                    .onFailure {
                        // The contribution landed; only the read-back failed. Not worth reporting
                        // as a sync failure, because nothing is outstanding.
                        return@withLock Result.success(Unit)
                    }

                Result.success(Unit)
            } finally {
                syncing.value = false
            }
        }
    }

    /** Refreshes the worldwide figures without publishing. Used when there is nothing to add. */
    suspend fun refreshFigures(): Result<Unit> {
        if (!backend.isConfigured) return Result.failure(BackendUnavailable)
        return backend.fetchFigures(clock.todayEpochDay()).map { cache(it) }
    }

    private suspend fun cache(figures: RemoteFigures) {
        syncDao.putSnapshot(
            RemoteSnapshotEntity(
                id = 0,
                globalTotal = figures.globalTotal,
                globalToday = figures.globalToday,
                participantCount = figures.participantCount,
                userTotal = figures.userTotal,
                userUid = accountRepository.state.first().uid,
                updatedAt = if (figures.updatedAt > 0) figures.updatedAt else clock.nowMillis(),
            )
        )
    }

    /**
     * Background work for the app's lifetime: publish shortly after counting settles, and retry
     * on a slow loop while anything is still owed to the world count.
     */
    fun start(scope: CoroutineScope) {
        if (!backend.isConfigured) return

        scope.launch {
            combine(
                accountRepository.state.map { it.uid }.distinctUntilChanged(),
                pendingTotal,
            ) { uid, pending -> uid to pending }
                .collectLatest { (uid, pending) ->
                    if (uid == null || pending <= 0) return@collectLatest
                    // Let a burst of counting settle before publishing, so a tasbih of a hundred
                    // is one upload rather than a hundred.
                    delay(SETTLE_MILLIS)
                    runCatching { syncNow() }
                }
        }

        scope.launch {
            var backoff = RETRY_FLOOR_MILLIS
            while (true) {
                delay(backoff)
                val account = accountRepository.state.first()
                val owed = pendingTotal.first()
                if (account.uid == null || owed <= 0) {
                    backoff = RETRY_FLOOR_MILLIS
                    continue
                }
                val succeeded = runCatching { syncNow().isSuccess }.getOrDefault(false)
                backoff = if (succeeded) {
                    RETRY_FLOOR_MILLIS
                } else {
                    (backoff * 2).coerceAtMost(RETRY_CEILING_MILLIS)
                }
            }
        }
    }

    companion object {
        private const val SETTLE_MILLIS = 2_500L
        private const val RETRY_FLOOR_MILLIS = 30_000L
        private const val RETRY_CEILING_MILLIS = 15 * 60_000L
    }
}

/** Exception text fit to show a user, without leaking a stack trace or an internal identifier. */
internal fun Throwable.userMessage(): String = when (this) {
    is BackendUnavailable -> "Cloud sync is not set up in this build."
    is NotSignedIn -> "Sign in to add your dhikr to the world count."
    else -> message?.take(160) ?: "Could not reach the world count."
}
