package com.lillah.dhikr.data.repository

import androidx.room.withTransaction
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.dao.CollectionDao
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.CounterDao
import com.lillah.dhikr.data.local.dao.DhikrDao
import com.lillah.dhikr.data.local.dao.SyncDao
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
import com.lillah.dhikr.data.local.toDomain
import com.lillah.dhikr.data.local.toEntity
import com.lillah.dhikr.data.seed.SeedData
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.DhikrCollection
import com.lillah.dhikr.domain.sync.SyncOperationKind
import com.lillah.dhikr.domain.sync.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Outcome of a single tap, so the UI knows whether to celebrate. */
data class CountResult(
    val currentCount: Int,
    val target: Int,
    val roundCompleted: Boolean,
    val roundsToday: Int,
)

class DhikrRepository(
    private val database: DhikrDatabase,
    private val dhikrDao: DhikrDao,
    private val collectionDao: CollectionDao,
    private val countDao: CountDao,
    private val counterDao: CounterDao,
    private val syncDao: SyncDao,
    private val profiles: ProfileRepository,
    private val clock: AppClock,
) {

    /** The profile whose data every query below is scoped to. */
    private val profileId: StateFlow<Long> get() = profiles.activeProfileId

    /**
     * Serialises the live round's read-modify-write. Rapid tapping is the normal case here, and
     * two taps that both read the same count would otherwise lose one of them. The day ledger
     * itself is safe without this — it increments in SQL — but the round state is not.
     */
    private val roundMutex = Mutex()

    fun observeAll(): Flow<List<Dhikr>> = profileId.flatMapLatest { pid ->
        dhikrDao.observeActive(pid).map { list -> list.map { it.toDomain() } }
    }

    fun observeArchived(): Flow<List<Dhikr>> = profileId.flatMapLatest { pid ->
        dhikrDao.observeArchived(pid).map { list -> list.map { it.toDomain() } }
    }

    fun observeFavorites(): Flow<List<Dhikr>> = profileId.flatMapLatest { pid ->
        dhikrDao.observeFavorites(pid).map { list -> list.map { it.toDomain() } }
    }

    fun observeByCollection(collectionId: Long): Flow<List<Dhikr>> =
        dhikrDao.observeByCollection(collectionId).map { list -> list.map { it.toDomain() } }

    fun observeDhikr(id: Long): Flow<Dhikr?> =
        dhikrDao.observeById(id).map { it?.toDomain() }

    suspend fun getDhikr(id: Long): Dhikr? = dhikrDao.getById(id)?.toDomain()

    fun observeCollections(): Flow<List<DhikrCollection>> = profileId.flatMapLatest { pid ->
        collectionDao.observeAll(pid).map { list -> list.map { it.toDomain() } }
    }

    fun observeCollection(id: Long): Flow<DhikrCollection?> =
        collectionDao.observeById(id).map { it?.toDomain() }

    suspend fun getCollection(id: Long): DhikrCollection? = collectionDao.getById(id)?.toDomain()

    suspend fun collectionIdOfKind(kind: CollectionKind): Long? =
        collectionDao.getByKind(profileId.value, kind.name)?.id

    /** Collections joined with today's completion, ready for the Collections grid. */
    fun observeCollectionProgress(): Flow<List<CollectionProgress>> =
        combine(profileId, today()) { pid, day -> pid to day }.flatMapLatest { (pid, day) ->
        combine(
            collectionDao.observeAll(pid),
            countDao.observeCollectionCompletions(pid, day),
        ) { collections, completions ->
            val byId = completions.associateBy { it.collectionId }
            collections.map { entity ->
                val stats = byId[entity.id]
                CollectionProgress(
                    collection = entity.toDomain(),
                    itemCount = stats?.itemCount ?: 0,
                    completedToday = stats?.completedCount ?: 0,
                    totalToday = stats?.totalToday ?: 0,
                )
            }
        }
    }

    /** Mirrors [StatsRepository]'s ticker so collection progress also rolls over at midnight. */
    private fun today(): Flow<Long> = flow {
        while (true) {
            emit(clock.todayEpochDay())
            val nextMidnight = clock.today().plusDays(1)
                .atStartOfDay(clock.zone()).toInstant().toEpochMilli()
            delay((nextMidnight - clock.nowMillis()).coerceIn(1_000L, 6 * 60 * 60 * 1000L))
        }
    }

    // ------------------------------------------------------------------ counting

    /**
     * Records one tap. Both halves of the write matter: the live round on the dhikr row so a
     * half-finished tasbih survives the process being killed, and the day ledger that every
     * statistic is built from.
     */
    suspend fun increment(dhikrId: Long, delta: Int = 1): CountResult? = roundMutex.withLock {
        val entity = dhikrDao.getById(dhikrId) ?: return@withLock null
        val target = entity.targetCount.coerceAtLeast(1)
        val today = clock.todayEpochDay()
        val now = clock.nowMillis()

        val roundsToday = if (entity.roundsEpochDay == today) entity.roundsToday else 0
        // A completed round stays on screen until the next tap, which then opens a new round.
        val base = if (entity.currentCount >= target) 0 else entity.currentCount
        val next = (base + delta).coerceAtMost(target)
        val completed = next >= target
        val rounds = if (completed) roundsToday + 1 else roundsToday

        // One transaction: the ledger and the outbox entry are either both durable or neither
        // is. A count that reached the device but not the queue would never reach the cloud.
        database.withTransaction {
            dhikrDao.updateRoundState(dhikrId, next, rounds, today, now)
            countDao.addCount(dhikrId, today, delta, now)
            syncDao.enqueue(
                countOperation(entity.id, entity.name, entity.profileId, today, delta.toLong(), now)
            )
        }
        counterDao.raiseTo(entity.profileId, CounterKeys.BEST_SESSION, next.toLong())

        CountResult(next, target, completed, rounds)
    }

    /** Undo. Rolls back into the previous round rather than sticking at zero. */
    suspend fun decrement(dhikrId: Long): CountResult? = roundMutex.withLock {
        val entity = dhikrDao.getById(dhikrId) ?: return@withLock null
        val target = entity.targetCount.coerceAtLeast(1)
        val today = clock.todayEpochDay()
        val now = clock.nowMillis()
        val roundsToday = if (entity.roundsEpochDay == today) entity.roundsToday else 0

        if (entity.currentCount == 0 && roundsToday == 0) {
            return@withLock CountResult(0, target, false, 0)
        }

        val (next, rounds) = if (entity.currentCount == 0) {
            (target - 1).coerceAtLeast(0) to (roundsToday - 1).coerceAtLeast(0)
        } else {
            val stepped = entity.currentCount - 1
            val rolledBack = if (entity.currentCount >= target) {
                (roundsToday - 1).coerceAtLeast(0)
            } else {
                roundsToday
            }
            stepped to rolledBack
        }

        database.withTransaction {
            dhikrDao.updateRoundState(dhikrId, next, rounds, today, now)
            countDao.addCount(dhikrId, today, -1, now)
            syncDao.enqueue(
                countOperation(entity.id, entity.name, entity.profileId, today, -1L, now)
            )
        }
        CountResult(next, target, false, rounds)
    }

    /**
     * A queued contribution. The id is a fresh UUID that also becomes the remote document id, so
     * a retried upload lands on the same document and is folded into the totals exactly once.
     */
    private fun countOperation(
        dhikrId: Long,
        dhikrName: String,
        profileId: Long,
        epochDay: Long,
        delta: Long,
        now: Long,
    ) = SyncOperationEntity(
        opId = UUID.randomUUID().toString(),
        kind = SyncOperationKind.COUNT_DELTA.name,
        dhikrId = dhikrId,
        dhikrName = dhikrName,
        epochDay = epochDay,
        delta = delta,
        createdAt = now,
        state = SyncState.PENDING.name,
        profileId = profileId,
    )

    /** Clears the live round only. Today's recorded total is history and is left alone. */
    suspend fun resetRound(dhikrId: Long) = dhikrDao.resetRound(dhikrId)

    // ------------------------------------------------------------------ editing

    suspend fun upsert(dhikr: Dhikr): Long {
        val isNew = dhikr.id == 0L
        val pid = profileId.value
        val entity = dhikr.toEntity().copy(
            sortOrder = if (isNew) dhikrDao.maxSortOrder(pid) + 1 else dhikr.sortOrder,
            createdAt = if (isNew) clock.nowMillis() else dhikr.createdAt,
            profileId = if (isNew) pid else dhikr.profileId,
        )
        val id = dhikrDao.upsert(entity)
        if (isNew && !dhikr.isBuiltIn) counterDao.increment(pid, CounterKeys.CUSTOM_DHIKR)
        return if (id > 0) id else dhikr.id
    }

    /** Changes the target and nothing else, so a live round in progress is never rolled back. */
    suspend fun setTarget(dhikrId: Long, target: Int) =
        dhikrDao.setTarget(dhikrId, target.coerceIn(1, 10_000))

    suspend fun setArchived(dhikrId: Long, archived: Boolean) =
        dhikrDao.setArchived(dhikrId, archived)
    suspend fun setFavorite(dhikrId: Long, favorite: Boolean) =
        dhikrDao.setFavorite(dhikrId, favorite)
    suspend fun reorder(idsInOrder: List<Long>) = dhikrDao.applyOrder(idsInOrder)

    suspend fun upsertCollection(collection: DhikrCollection): Long {
        val isNew = collection.id == 0L
        val pid = profileId.value
        val entity = collection.toEntity().copy(
            sortOrder = if (isNew) collectionDao.maxSortOrder(pid) + 1 else collection.sortOrder,
            profileId = if (isNew) pid else collection.profileId,
        )
        val id = collectionDao.upsert(entity)
        if (isNew && !collection.isBuiltIn) {
            counterDao.increment(pid, CounterKeys.CUSTOM_COLLECTIONS)
        }
        return if (id > 0) id else collection.id
    }

    suspend fun setCollectionArchived(id: Long, archived: Boolean) =
        collectionDao.setArchived(id, archived)

    suspend fun setCollectionCover(id: Long, path: String?) = collectionDao.setCoverImage(id, path)

    /** Every cover still referenced by a collection, for pruning orphaned files. */
    suspend fun referencedCoverPaths(): List<String> = collectionDao.coverImagePaths()

    // ------------------------------------------------------------------ seeding

    /** Inserts the shipped content. Safe to call again: it only adds what is missing. */
    suspend fun seedIfEmpty(profileId: Long = this.profileId.value) {
        if (collectionDao.count(profileId) > 0 || dhikrDao.count(profileId) > 0) return
        installSeedContent(profileId)
    }

    /**
     * Adds back any shipped adhkar the user has removed, and nothing else.
     *
     * Restore is additive on purpose. Deleting and re-inserting the built-ins would cascade away
     * their recorded counts, so anything already present — even if edited — is left untouched.
     *
     * @return how many adhkar were restored.
     */
    suspend fun restoreDefaults(): Int = installSeedContent(profileId.value)

    private suspend fun installSeedContent(profileId: Long): Int {
        val morningId = ensureCollection(profileId, CollectionKind.Morning, seedIndex = 0)
        val eveningId = ensureCollection(profileId, CollectionKind.Evening, seedIndex = 1)
        val essentialsId = ensureCollection(profileId, CollectionKind.Essentials, seedIndex = 2)
        val afterPrayerId = ensureCollection(profileId, CollectionKind.AfterPrayer, seedIndex = 3)

        return addMissing(profileId, essentialsId, SeedData.essentials(essentialsId)) +
            addMissing(profileId, morningId, SeedData.morning(morningId)) +
            addMissing(profileId, eveningId, SeedData.evening(eveningId)) +
            addMissing(profileId, afterPrayerId, SeedData.afterPrayer(afterPrayerId))
    }

    private suspend fun addMissing(
        profileId: Long,
        collectionId: Long,
        seeds: List<com.lillah.dhikr.data.local.entity.DhikrEntity>,
    ): Int {
        val present = dhikrDao.namesInCollection(collectionId).toHashSet()
        val missing = seeds
            .filter { it.name !in present }
            .map { it.copy(profileId = profileId) }
        if (missing.isNotEmpty()) dhikrDao.insertAll(missing)
        return missing.size
    }

    private suspend fun ensureCollection(
        profileId: Long,
        kind: CollectionKind,
        seedIndex: Int,
    ): Long {
        collectionDao.getByKind(profileId, kind.name)?.let { return it.id }
        return collectionDao.upsert(SeedData.collections[seedIndex].copy(profileId = profileId))
    }
}

object CounterKeys {
    const val MORNING_COMPLETIONS = "morning_completions"
    const val EVENING_COMPLETIONS = "evening_completions"
    const val MORNING_LAST_DAY = "morning_last_day"
    const val EVENING_LAST_DAY = "evening_last_day"
    const val GOAL_DAYS = "goal_days"
    const val GOAL_LAST_DAY = "goal_last_day"
    const val CUSTOM_DHIKR = "custom_dhikr"
    const val CUSTOM_COLLECTIONS = "custom_collections"
    const val BEST_SESSION = "best_session"
    const val BEST_DAY = "best_day"
    const val BEST_STREAK = "best_streak"
}
