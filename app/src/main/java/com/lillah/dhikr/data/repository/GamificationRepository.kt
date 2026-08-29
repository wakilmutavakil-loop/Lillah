package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.dao.AchievementDao
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.CounterDao
import com.lillah.dhikr.data.local.entity.AchievementEntity
import com.lillah.dhikr.domain.gamification.AchievementCatalog
import com.lillah.dhikr.domain.gamification.AchievementDef
import com.lillah.dhikr.domain.gamification.AchievementStatus
import com.lillah.dhikr.domain.gamification.GamificationSnapshot
import com.lillah.dhikr.domain.gamification.Streaks
import com.lillah.dhikr.domain.model.CollectionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Owns the "did anything change?" side of gamification.
 *
 * Everything that can be re-derived from the count ledger is re-derived; only facts with no
 * durable trace — how many times a collection was finished, how many days a goal that has since
 * changed was met — are kept as counters.
 */
class GamificationRepository(
    private val achievementDao: AchievementDao,
    private val counterDao: CounterDao,
    private val countDao: CountDao,
    private val dhikrRepository: DhikrRepository,
    private val clock: AppClock,
) {

    fun observeStatuses(): Flow<List<AchievementStatus>> = combine(
        observeSnapshot(),
        achievementDao.observeAll(),
    ) { snapshot, unlocked ->
        AchievementCatalog.statuses(snapshot, unlocked.associate { it.key to it.unlockedAt })
    }

    fun observeUnlockedCount(): Flow<Int> = achievementDao.observeAll().map { it.size }

    /** Newly unlocked achievements the UI has not celebrated yet. */
    fun observePendingCelebrations(): Flow<List<AchievementDef>> =
        achievementDao.observeUncelebrated().map { rows ->
            rows.mapNotNull { AchievementCatalog.find(it.key) }
        }

    suspend fun markCelebrated(key: String) = achievementDao.markCelebrated(key)

    fun observeSnapshot(): Flow<GamificationSnapshot> = combine(
        countDao.observeLifetimeTotal(),
        countDao.observeActiveDays(),
        counterDao.observeAll(),
    ) { lifetime, activeDays, counters ->
        val values = counters.associate { it.key to it.value }
        val streak = Streaks.compute(activeDays, clock.todayEpochDay())
        GamificationSnapshot(
            lifetimeTotal = lifetime,
            currentStreak = streak.current,
            bestStreak = maxOf(streak.best.toLong(), values[CounterKeys.BEST_STREAK] ?: 0L).toInt(),
            activeDays = activeDays.size,
            morningCompletions = values[CounterKeys.MORNING_COMPLETIONS] ?: 0,
            eveningCompletions = values[CounterKeys.EVENING_COMPLETIONS] ?: 0,
            goalDays = values[CounterKeys.GOAL_DAYS] ?: 0,
            customDhikrCreated = values[CounterKeys.CUSTOM_DHIKR] ?: 0,
            collectionsCreated = values[CounterKeys.CUSTOM_COLLECTIONS] ?: 0,
            bestSession = values[CounterKeys.BEST_SESSION] ?: 0,
            bestDay = values[CounterKeys.BEST_DAY] ?: 0,
        )
    }

    /**
     * Called after every committed count. Updates the day-scoped counters, then unlocks whatever
     * the new snapshot satisfies. Unlocks are inserted with IGNORE, so this is idempotent.
     */
    suspend fun refresh(dailyGoal: Int) {
        val today = clock.todayEpochDay()
        val dayTotal = countDao.dayTotal(today)

        counterDao.raiseTo(CounterKeys.BEST_DAY, dayTotal.toLong())

        if (dayTotal >= dailyGoal && dailyGoal > 0) {
            creditOncePerDay(CounterKeys.GOAL_LAST_DAY, CounterKeys.GOAL_DAYS, today)
        }

        creditCollectionIfComplete(
            CollectionKind.Morning,
            CounterKeys.MORNING_LAST_DAY,
            CounterKeys.MORNING_COMPLETIONS,
            today,
        )
        creditCollectionIfComplete(
            CollectionKind.Evening,
            CounterKeys.EVENING_LAST_DAY,
            CounterKeys.EVENING_COMPLETIONS,
            today,
        )

        val streak = Streaks.compute(countDao.activeDays(), today)
        counterDao.raiseTo(CounterKeys.BEST_STREAK, streak.best.toLong())

        unlockSatisfied()
    }

    private suspend fun unlockSatisfied() {
        val snapshot = currentSnapshot()
        val now = clock.nowMillis()
        AchievementCatalog.satisfied(snapshot).forEach { key ->
            achievementDao.insertIgnore(AchievementEntity(key = key, unlockedAt = now))
        }
    }

    private suspend fun currentSnapshot(): GamificationSnapshot {
        val values = counterDao.getAll().associate { it.key to it.value }
        val activeDays = countDao.activeDays()
        val streak = Streaks.compute(activeDays, clock.todayEpochDay())
        return GamificationSnapshot(
            lifetimeTotal = countDao.lifetimeTotal(),
            currentStreak = streak.current,
            bestStreak = streak.best,
            activeDays = activeDays.size,
            morningCompletions = values[CounterKeys.MORNING_COMPLETIONS] ?: 0,
            eveningCompletions = values[CounterKeys.EVENING_COMPLETIONS] ?: 0,
            goalDays = values[CounterKeys.GOAL_DAYS] ?: 0,
            customDhikrCreated = values[CounterKeys.CUSTOM_DHIKR] ?: 0,
            collectionsCreated = values[CounterKeys.CUSTOM_COLLECTIONS] ?: 0,
            bestSession = values[CounterKeys.BEST_SESSION] ?: 0,
            bestDay = values[CounterKeys.BEST_DAY] ?: 0,
        )
    }

    private suspend fun creditCollectionIfComplete(
        kind: CollectionKind,
        lastDayKey: String,
        counterKey: String,
        today: Long,
    ) {
        val collectionId = dhikrRepository.collectionIdOfKind(kind) ?: return
        val completion = countDao.collectionCompletion(collectionId, today) ?: return
        if (completion.itemCount == 0) return
        if (completion.completedCount < completion.itemCount) return
        creditOncePerDay(lastDayKey, counterKey, today)
    }

    /** Guards a counter so a given day can only ever contribute once to it. */
    private suspend fun creditOncePerDay(lastDayKey: String, counterKey: String, today: Long) {
        val last = counterDao.get(lastDayKey)
        if (last == today) return
        counterDao.increment(counterKey)
        counterDao.insertIgnore(
            com.lillah.dhikr.data.local.entity.CounterEntity(lastDayKey, today)
        )
        counterDao.setValue(lastDayKey, today)
    }

    suspend fun resetAll() {
        achievementDao.clearAll()
        counterDao.clearAll()
    }
}
