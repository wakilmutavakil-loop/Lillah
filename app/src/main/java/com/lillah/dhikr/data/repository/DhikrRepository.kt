package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.dao.CollectionDao
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.CounterDao
import com.lillah.dhikr.data.local.dao.DhikrDao
import com.lillah.dhikr.data.local.toDomain
import com.lillah.dhikr.data.local.toEntity
import com.lillah.dhikr.data.seed.SeedData
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.DhikrCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Outcome of a single tap, so the UI knows whether to celebrate. */
data class CountResult(
    val currentCount: Int,
    val target: Int,
    val roundCompleted: Boolean,
    val roundsToday: Int,
)

class DhikrRepository(
    private val dhikrDao: DhikrDao,
    private val collectionDao: CollectionDao,
    private val countDao: CountDao,
    private val counterDao: CounterDao,
    private val clock: AppClock,
) {

    fun observeAll(): Flow<List<Dhikr>> =
        dhikrDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Dhikr>> =
        dhikrDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeFavorites(): Flow<List<Dhikr>> =
        dhikrDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    fun observeByCollection(collectionId: Long): Flow<List<Dhikr>> =
        dhikrDao.observeByCollection(collectionId).map { list -> list.map { it.toDomain() } }

    fun observeDhikr(id: Long): Flow<Dhikr?> =
        dhikrDao.observeById(id).map { it?.toDomain() }

    suspend fun getDhikr(id: Long): Dhikr? = dhikrDao.getById(id)?.toDomain()

    suspend fun firstDhikrId(): Long? = dhikrDao.getFirstActive()?.id

    fun observeCollections(): Flow<List<DhikrCollection>> =
        collectionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCollection(id: Long): Flow<DhikrCollection?> =
        collectionDao.observeById(id).map { it?.toDomain() }

    suspend fun getCollection(id: Long): DhikrCollection? = collectionDao.getById(id)?.toDomain()

    suspend fun collectionIdOfKind(kind: CollectionKind): Long? =
        collectionDao.getByKind(kind.name)?.id

    /** Collections joined with today's completion, ready for the Collections grid. */
    fun observeCollectionProgress(): Flow<List<CollectionProgress>> {
        val today = clock.todayEpochDay()
        return combine(
            collectionDao.observeAll(),
            countDao.observeCollectionCompletions(today),
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

    // ------------------------------------------------------------------ counting

    /**
     * Records one tap. Both halves of the write matter: the live round on the dhikr row so a
     * half-finished tasbih survives the process being killed, and the day ledger that every
     * statistic is built from.
     */
    suspend fun increment(dhikrId: Long, delta: Int = 1): CountResult? {
        val entity = dhikrDao.getById(dhikrId) ?: return null
        val target = entity.targetCount.coerceAtLeast(1)
        val today = clock.todayEpochDay()
        val now = clock.nowMillis()

        val roundsToday = if (entity.roundsEpochDay == today) entity.roundsToday else 0
        // A completed round stays on screen until the next tap, which then opens a new round.
        val base = if (entity.currentCount >= target) 0 else entity.currentCount
        val next = (base + delta).coerceAtMost(target)
        val completed = next >= target
        val rounds = if (completed) roundsToday + 1 else roundsToday

        dhikrDao.updateRoundState(dhikrId, next, rounds, today, now)
        countDao.addCount(dhikrId, today, delta, now)
        counterDao.raiseTo(CounterKeys.BEST_SESSION, next.toLong())

        return CountResult(next, target, completed, rounds)
    }

    /** Undo. Rolls back into the previous round rather than sticking at zero. */
    suspend fun decrement(dhikrId: Long): CountResult? {
        val entity = dhikrDao.getById(dhikrId) ?: return null
        val target = entity.targetCount.coerceAtLeast(1)
        val today = clock.todayEpochDay()
        val now = clock.nowMillis()
        val roundsToday = if (entity.roundsEpochDay == today) entity.roundsToday else 0

        if (entity.currentCount == 0 && roundsToday == 0) {
            return CountResult(0, target, false, 0)
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

        dhikrDao.updateRoundState(dhikrId, next, rounds, today, now)
        countDao.addCount(dhikrId, today, -1, now)
        return CountResult(next, target, false, rounds)
    }

    /** Clears the live round only. Today's recorded total is history and is left alone. */
    suspend fun resetRound(dhikrId: Long) = dhikrDao.resetRound(dhikrId)

    // ------------------------------------------------------------------ editing

    suspend fun upsert(dhikr: Dhikr): Long {
        val isNew = dhikr.id == 0L
        val entity = dhikr.toEntity().copy(
            sortOrder = if (isNew) dhikrDao.maxSortOrder() + 1 else dhikr.sortOrder,
            createdAt = if (isNew) clock.nowMillis() else dhikr.createdAt,
        )
        val id = dhikrDao.insert(entity)
        if (isNew && !dhikr.isBuiltIn) counterDao.increment(CounterKeys.CUSTOM_DHIKR)
        return if (id > 0) id else dhikr.id
    }

    suspend fun delete(dhikrId: Long) = dhikrDao.deleteById(dhikrId)
    suspend fun setArchived(dhikrId: Long, archived: Boolean) =
        dhikrDao.setArchived(dhikrId, archived)
    suspend fun setFavorite(dhikrId: Long, favorite: Boolean) =
        dhikrDao.setFavorite(dhikrId, favorite)
    suspend fun setDailyTarget(dhikrId: Long, dailyTarget: Int?) =
        dhikrDao.setDailyTarget(dhikrId, dailyTarget)
    suspend fun reorder(idsInOrder: List<Long>) = dhikrDao.applyOrder(idsInOrder)

    suspend fun upsertCollection(collection: DhikrCollection): Long {
        val isNew = collection.id == 0L
        val entity = collection.toEntity().copy(
            sortOrder = if (isNew) collectionDao.maxSortOrder() + 1 else collection.sortOrder,
        )
        val id = collectionDao.insert(entity)
        if (isNew && !collection.isBuiltIn) counterDao.increment(CounterKeys.CUSTOM_COLLECTIONS)
        return if (id > 0) id else collection.id
    }

    suspend fun deleteCollection(id: Long) = collectionDao.deleteById(id)
    suspend fun setCollectionCover(id: Long, path: String?) = collectionDao.setCoverImage(id, path)
    suspend fun reorderCollections(idsInOrder: List<Long>) = collectionDao.applyOrder(idsInOrder)

    // ------------------------------------------------------------------ seeding

    /** Inserts the shipped content. Safe to call again: it only adds what is missing. */
    suspend fun seedIfEmpty() {
        if (collectionDao.count() > 0 || dhikrDao.count() > 0) return
        installSeedContent()
    }

    /**
     * Adds back any shipped adhkar the user has removed, and nothing else.
     *
     * Restore is additive on purpose. Deleting and re-inserting the built-ins would cascade away
     * their recorded counts, so anything already present — even if edited — is left untouched.
     *
     * @return how many adhkar were restored.
     */
    suspend fun restoreDefaults(): Int = installSeedContent()

    private suspend fun installSeedContent(): Int {
        val morningId = ensureCollection(CollectionKind.Morning, seedIndex = 0)
        val eveningId = ensureCollection(CollectionKind.Evening, seedIndex = 1)
        val essentialsId = ensureCollection(CollectionKind.Essentials, seedIndex = 2)
        val afterPrayerId = ensureCollection(CollectionKind.AfterPrayer, seedIndex = 3)

        return addMissing(essentialsId, SeedData.essentials(essentialsId)) +
            addMissing(morningId, SeedData.morning(morningId)) +
            addMissing(eveningId, SeedData.evening(eveningId)) +
            addMissing(afterPrayerId, SeedData.afterPrayer(afterPrayerId))
    }

    private suspend fun addMissing(
        collectionId: Long,
        seeds: List<com.lillah.dhikr.data.local.entity.DhikrEntity>,
    ): Int {
        val present = dhikrDao.namesInCollection(collectionId).toHashSet()
        val missing = seeds.filter { it.name !in present }
        if (missing.isNotEmpty()) dhikrDao.insertAll(missing)
        return missing.size
    }

    private suspend fun ensureCollection(kind: CollectionKind, seedIndex: Int): Long {
        collectionDao.getByKind(kind.name)?.let { return it.id }
        return collectionDao.insert(SeedData.collections[seedIndex])
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
