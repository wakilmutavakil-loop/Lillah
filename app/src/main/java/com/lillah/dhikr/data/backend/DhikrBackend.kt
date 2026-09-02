package com.lillah.dhikr.data.backend

import android.app.Activity
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
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
     * Uploads operations. Implementations must be idempotent on [SyncOperationEntity.opId]:
     * pushing the same operation again, after a timeout or a crash, must not count it twice.
     */
    suspend fun push(uid: String, operations: List<SyncOperationEntity>): Result<Unit>

    /** Records or refreshes the account's profile document. */
    suspend fun registerUser(user: AuthUser): Result<Unit>

    suspend fun fetchFigures(uid: String?): Result<RemoteFigures>

    /** Live global and personal figures. Emits nothing when unconfigured. */
    fun observeFigures(uid: String?): Flow<RemoteFigures>
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

    override suspend fun push(uid: String, operations: List<SyncOperationEntity>) =
        Result.failure<Unit>(BackendUnavailable)

    override suspend fun registerUser(user: AuthUser) = Result.failure<Unit>(BackendUnavailable)

    override suspend fun fetchFigures(uid: String?) =
        Result.failure<RemoteFigures>(BackendUnavailable)

    override fun observeFigures(uid: String?): Flow<RemoteFigures> = flowOf()
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
