package com.lillah.dhikr.data.sync

import com.lillah.dhikr.data.backend.DhikrBackend
import com.lillah.dhikr.domain.sync.RemoteFigures

/**
 * Stands in for Firestore, modelling the property that matters: a contribution is a document
 * holding one absolute number, so writing it again simply overwrites it.
 */
class FakeBackend(
    override val isConfigured: Boolean = true,
) : DhikrBackend {

    /** uid to published total — exactly the shape of the real `contributions` collection. */
    val contributions = linkedMapOf<String, Long>()
    val todayTotals = linkedMapOf<String, Long>()

    /** Every write attempt, so a test can assert how many documents a sync actually costs. */
    var writeCount = 0
        private set

    var failNextPublish: Throwable? = null
    var failNextFetch: Throwable? = null

    /** What the server would compute: a sum across the collection. */
    val worldTotal: Long get() = contributions.values.sum()

    override suspend fun publishContribution(
        uid: String,
        total: Long,
        todayTotal: Long,
        todayEpochDay: Long,
    ): Result<Unit> {
        failNextPublish?.let {
            failNextPublish = null
            return Result.failure(it)
        }
        writeCount++
        contributions[uid] = total
        todayTotals[uid] = todayTotal
        return Result.success(Unit)
    }

    override suspend fun fetchFigures(todayEpochDay: Long): Result<RemoteFigures> {
        failNextFetch?.let {
            failNextFetch = null
            return Result.failure(it)
        }
        return Result.success(
            RemoteFigures(
                globalTotal = worldTotal,
                globalToday = todayTotals.values.sum(),
                participantCount = contributions.size.toLong(),
                userTotal = 0,
                updatedAt = 1_000L,
            )
        )
    }
}
