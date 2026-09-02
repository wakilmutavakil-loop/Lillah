package com.lillah.dhikr.data.sync

import com.lillah.dhikr.data.backend.DhikrBackend
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
import com.lillah.dhikr.domain.sync.AuthUser
import com.lillah.dhikr.domain.sync.RemoteFigures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Stands in for Firestore, and models the property that matters: the server keys operations by
 * their id, so pushing one twice must leave the totals unchanged.
 */
class FakeBackend(
    override val isConfigured: Boolean = true,
) : DhikrBackend {

    /** Every operation the server has accepted, keyed by id — exactly like a document store. */
    val accepted = linkedMapOf<String, SyncOperationEntity>()

    /** Every push attempt, including duplicates, so tests can assert on retry behaviour. */
    val pushAttempts = mutableListOf<List<String>>()

    var failNextPush: Throwable? = null
    var registeredUsers = mutableListOf<AuthUser>()

    private val figures = MutableStateFlow<RemoteFigures?>(null)

    /** What the server would compute: the sum over distinct accepted operations. */
    val serverUserTotal: Long get() = accepted.values.sumOf { it.delta }

    override suspend fun push(
        uid: String,
        operations: List<SyncOperationEntity>,
    ): Result<Unit> {
        pushAttempts += operations.map { it.opId }
        failNextPush?.let {
            failNextPush = null
            return Result.failure(it)
        }
        operations.forEach { accepted[it.opId] = it }
        figures.value = RemoteFigures(
            globalTotal = serverUserTotal,
            userTotal = serverUserTotal,
            participantCount = 1,
            updatedAt = 1_000L,
        )
        return Result.success(Unit)
    }

    override suspend fun registerUser(user: AuthUser): Result<Unit> {
        registeredUsers += user
        return Result.success(Unit)
    }

    override suspend fun fetchFigures(uid: String?): Result<RemoteFigures> = Result.success(
        RemoteFigures(
            globalTotal = serverUserTotal,
            userTotal = serverUserTotal,
            participantCount = 1,
            updatedAt = 1_000L,
        )
    )

    override fun observeFigures(uid: String?): Flow<RemoteFigures> = figures.filterNotNull()
}
