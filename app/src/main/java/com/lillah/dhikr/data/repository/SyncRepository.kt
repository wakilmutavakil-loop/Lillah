package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.backend.BackendUnavailable
import com.lillah.dhikr.data.backend.DhikrBackend
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.SyncDao
import com.lillah.dhikr.data.local.entity.RemoteSnapshotEntity
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.domain.sync.AuthUser
import com.lillah.dhikr.domain.sync.RemoteFigures
import com.lillah.dhikr.domain.sync.SyncOperationKind
import com.lillah.dhikr.domain.sync.SyncState
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

object NotSignedIn : Exception("Sign in to sync your dhikr to your account.")

/**
 * Moves queued operations to the cloud, and cloud figures back.
 *
 * Three rules shape everything here:
 *
 *  - A local count is never contingent on the network. Counting writes to the outbox and returns;
 *    this class drains it whenever it can, and failing to drain it is not an error the user needs
 *    to act on.
 *  - Nothing is ever discarded. A failed push increments an attempt counter and stays queued.
 *    There is no path in this file that deletes an unsynced operation.
 *  - Uploads are idempotent. Every operation carries a client-generated id that the backend uses
 *    as its document id, so a retry after a timeout re-writes the same document rather than
 *    adding a second contribution.
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

    val status: Flow<SyncStatus> = combine(
        accountRepository.state,
        profiles.activeProfileId.flatMapLatest { syncDao.observePendingCount(it) },
        profiles.activeProfileId.flatMapLatest { syncDao.observePendingTotal(it) },
        syncing,
    ) { account, pending, pendingTotal, isSyncing ->
        SyncStatus(
            backendConfigured = backend.isConfigured,
            signedIn = account.isSignedIn,
            syncing = isSyncing,
            pendingOperations = pending,
            pendingTotal = pendingTotal,
            lastSyncAt = account.lastSyncAt,
            lastError = account.lastSyncError,
        )
    }

    /** Last known cloud figures, served from cache so the board renders offline. */
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
     * Attributes history this device accumulated before it was ever attached to an account.
     *
     * The amount is the local lifetime total minus everything the outbox has ever carried, which
     * is precisely the counting that happened before this feature existed. It is queued as a
     * single operation whose id is derived from the device, so running this again — on every
     * sign-in, after a reinstall of the app, on a second sign-in to the same account — inserts
     * nothing new and cannot double count.
     */
    suspend fun claimExistingHistory() {
        val pid = profileId
        val deviceId = accountRepository.deviceId()
        val opId = baselineOpId(deviceId, pid)
        if (syncDao.exists(opId)) return

        val lifetime = countDao.lifetimeTotal(pid)
        val alreadyQueued = syncDao.countDeltaTotal(pid)
        val baseline = (lifetime - alreadyQueued).coerceAtLeast(0)
        if (baseline <= 0) return

        syncDao.enqueue(
            SyncOperationEntity(
                opId = opId,
                kind = SyncOperationKind.BASELINE.name,
                dhikrId = null,
                dhikrName = null,
                epochDay = clock.todayEpochDay(),
                delta = baseline,
                createdAt = clock.nowMillis(),
                state = SyncState.PENDING.name,
                profileId = pid,
            )
        )
    }

    suspend fun registerUser(user: AuthUser) {
        backend.registerUser(user)
    }

    /**
     * Drains the outbox, then refreshes the cached figures.
     *
     * Returns success when there was nothing to do, so callers can invoke it freely. A concurrent
     * call returns immediately rather than queueing a second drain.
     */
    suspend fun syncNow(): Result<Unit> {
        if (!backend.isConfigured) return Result.failure(BackendUnavailable)
        val account = accountRepository.state.first()
        val uid = account.uid ?: return Result.failure(NotSignedIn)
        if (syncMutex.isLocked) return Result.success(Unit)

        return syncMutex.withLock {
            syncing.value = true
            try {
                drain(uid).onFailure { error ->
                    accountRepository.recordSyncFailure(error.userMessage())
                    return@withLock Result.failure(error)
                }

                backend.fetchFigures(uid)
                    .onSuccess { cache(it, uid) }
                    .onFailure { error ->
                        // The upload succeeded; only the read-back failed. Not worth reporting as
                        // a sync failure, because nothing is at risk.
                        accountRepository.recordSyncSuccess(clock.nowMillis())
                        return@withLock Result.failure(error)
                    }

                accountRepository.recordSyncSuccess(clock.nowMillis())
                Result.success(Unit)
            } finally {
                syncing.value = false
            }
        }
    }

    private suspend fun drain(uid: String): Result<Unit> {
        val pid = profileId
        while (true) {
            val batch = syncDao.pending(pid, BATCH_SIZE)
            if (batch.isEmpty()) return Result.success(Unit)

            val ids = batch.map { it.opId }
            val result = backend.push(uid, batch)
            val now = clock.nowMillis()

            if (result.isFailure) {
                // Stays queued. The attempt counter is what drives the backoff, and nothing here
                // removes the operation, so an outage costs a delay and never a contribution.
                syncDao.markFailed(ids, result.exceptionOrNull()?.userMessage(), now)
                return Result.failure(result.exceptionOrNull() ?: BackendUnavailable)
            }
            syncDao.markSynced(ids, uid, now)
        }
    }

    private suspend fun cache(figures: RemoteFigures, uid: String?) {
        syncDao.putSnapshot(
            RemoteSnapshotEntity(
                id = 0,
                globalTotal = figures.globalTotal,
                globalToday = figures.globalToday,
                participantCount = figures.participantCount,
                userTotal = figures.userTotal,
                userUid = uid,
                updatedAt = if (figures.updatedAt > 0) figures.updatedAt else clock.nowMillis(),
            )
        )
    }

    /**
     * Background work for the app's lifetime: drain shortly after counting settles, retry on a
     * slow loop while anything is queued, and keep the cached figures fresh from the live feed.
     */
    fun start(scope: CoroutineScope) {
        if (!backend.isConfigured) return

        scope.launch {
            combine(
                accountRepository.state.map { it.uid }.distinctUntilChanged(),
                profiles.activeProfileId.flatMapLatest { syncDao.observePendingCount(it) },
            ) { uid, pending -> uid to pending }
                .collectLatest { (uid, pending) ->
                    if (uid == null || pending == 0) return@collectLatest
                    // Let a burst of taps settle before uploading, so a tasbih of 100 is a
                    // handful of batched writes rather than a hundred round trips.
                    delay(SETTLE_MILLIS)
                    runCatching { syncNow() }
                }
        }

        scope.launch {
            var backoff = RETRY_FLOOR_MILLIS
            while (true) {
                delay(backoff)
                val account = accountRepository.state.first()
                val pending = syncDao.pending(profileId, limit = 1)
                if (account.uid == null || pending.isEmpty()) {
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

        scope.launch {
            accountRepository.state.map { it.uid }.distinctUntilChanged().collectLatest { uid ->
                backend.observeFigures(uid).collect { figures -> cache(figures, uid) }
            }
        }
    }

    companion object {
        const val BATCH_SIZE = 200
        private const val SETTLE_MILLIS = 2_500L
        private const val RETRY_FLOOR_MILLIS = 30_000L
        private const val RETRY_CEILING_MILLIS = 15 * 60_000L

        /** Scoped to the profile as well as the device: two people on one phone each claim once. */
        fun baselineOpId(deviceId: String, profileId: Long) = "baseline-$deviceId-p$profileId"
    }
}

/** Exception text fit to show a user, without leaking a stack trace or an internal identifier. */
internal fun Throwable.userMessage(): String = when (this) {
    is BackendUnavailable -> "Cloud sync is not set up in this build."
    is NotSignedIn -> "Sign in to sync."
    else -> message?.take(160) ?: "Sync could not complete."
}
