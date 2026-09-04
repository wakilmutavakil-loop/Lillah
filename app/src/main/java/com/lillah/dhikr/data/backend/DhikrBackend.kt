package com.lillah.dhikr.data.backend

import android.app.Activity
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.AuthUser
import com.lillah.dhikr.domain.sync.RemoteFigures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The cloud, as the rest of the app sees it.
 *
 * Behind an interface for one practical reason: the app must build, install and run with no
 * backend credentials at all. [UnconfiguredBackend] is a complete, honest implementation of "there
 * is no cloud", so nothing above this layer needs a null check or a feature flag.
 */
interface DhikrBackend {

    /** False when no project credentials were supplied at build time. */
    val isConfigured: Boolean

    /**
     * Publishes this account's running total.
     *
     * An absolute figure, not a delta — which makes the upload idempotent by construction. Sending
     * the same total twice, after a timeout or a crash or a hundred retries, leaves the cloud
     * exactly where it was. There is no way for a repeated upload to count anything twice, because
     * there is nothing being added.
     *
     * One document write per connect, whatever the user counted in between.
     */
    suspend fun publishContribution(
        uid: String,
        total: Long,
        todayTotal: Long,
        todayEpochDay: Long,
    ): Result<Unit>

    /**
     * Reads the worldwide figures, aggregated server-side — nothing is downloaded per person.
     *
     * [todayEpochDay] is the caller's local day, so "counted worldwide today" means the people
     * whose day matches this one rather than a UTC day nobody is actually living in.
     */
    suspend fun fetchFigures(todayEpochDay: Long): Result<RemoteFigures>
}

interface AuthGateway {

    val isConfigured: Boolean

    /** Which sign-in buttons to offer. Empty when nothing is configured. */
    val availableMethods: List<AuthMethod>

    /** The signed-in user, or null. Survives process death via the SDK's own persistence. */
    fun currentUser(): Flow<AuthUser?>

    suspend fun signIn(activity: Activity, method: AuthMethod): Result<AuthUser>

    /** Ends the session. Must not touch local data. */
    suspend fun signOut()
}

/**
 * What the app is without a backend: a fully working offline dhikr counter.
 *
 * Every call fails or returns empty rather than throwing, so an unconfigured build behaves like a
 * device that is permanently offline — which is a state the sync engine already handles.
 */
class UnconfiguredBackend : DhikrBackend {
    override val isConfigured = false

    override suspend fun publishContribution(
        uid: String,
        total: Long,
        todayTotal: Long,
        todayEpochDay: Long,
    ) = Result.failure<Unit>(BackendUnavailable)

    override suspend fun fetchFigures(todayEpochDay: Long) =
        Result.failure<RemoteFigures>(BackendUnavailable)
}

class UnconfiguredAuthGateway : AuthGateway {
    override val isConfigured = false
    override val availableMethods: List<AuthMethod> = emptyList()
    override fun currentUser(): Flow<AuthUser?> = flowOf(null)
    override suspend fun signIn(activity: Activity, method: AuthMethod) =
        Result.failure<AuthUser>(BackendUnavailable)

    override suspend fun signOut() = Unit
}

object BackendUnavailable : Exception(
    "Cloud sync is not configured in this build. Dhikr are still counted and stored on this device."
)
