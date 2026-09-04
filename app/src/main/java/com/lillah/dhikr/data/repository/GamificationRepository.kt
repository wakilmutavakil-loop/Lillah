package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.dao.AchievementDao
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.CounterDao
import com.lillah.dhikr.data.local.entity.ProfileAchievementEntity
import com.lillah.dhikr.data.local.entity.ProfileCounterEntity
import com.lillah.dhikr.domain.gamification.AchievementCatalog
import com.lillah.dhikr.domain.gamification.AchievementDef
import com.lillah.dhikr.domain.gamification.AchievementStatus
import com.lillah.dhikr.domain.gamification.GamificationSnapshot
import com.lillah.dhikr.domain.gamification.Streaks
import com.lillah.dhikr.domain.model.CollectionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val profiles: ProfileRepository,
    private val clock: AppClock,
) {

    private val profileId: Long get() = profiles.activeProfileId.value

    fun observeStatuses(): Flow<List<AchievementStatus>> =
        profiles.activeProfileId.flatMapLatest { pid ->
            combine(observeSnapshot(), achievementDao.observeAll(pid)) { snapshot, unlocked ->
                AchievementCatalog.statuses(
                    snapshot,
                    unlocked.associate { it.key to it.unlockedAt },
                )
            }
        }

    /** Newly unlocked achievements the UI has not celebrated yet. */
    fun observePendingCelebrations(): Flow<List<AchievementDef>> =
        profiles.activeProfileId.flatMapLatest { pid ->
            achievementDao.observeUncelebrated(pid).map { rows ->
                rows.mapNotNull { AchievementCatalog.find(it.key) }
            }
        }

    suspend fun markCelebrated(key: String) = achievementDao.markCelebrated(profileId, key)

    fun observeSnapshot(): Flow<GamificationSnapshot> = profiles.activeProfileId
        .flatMapLatest { pid -> snapshotFor(pid) }

    private fun snapshotFor(pid: Long): Flow<GamificationSnapshot> = combine(
        countDao.observeLifetimeTotal(pid),
        countDao.observeActiveDays(pid),
        counterDao.observeAll(pid),
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
        val pid = profileId
        val today = clock.todayEpochDay()
        val dayTotal = countDao.dayTotal(pid, today)

        counterDao.raiseTo(pid, CounterKeys.BEST_DAY, dayTotal.toLong())

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

        val streak = Streaks.compute(countDao.activeDays(pid), today)
        counterDao.raiseTo(pid, CounterKeys.BEST_STREAK, streak.best.toLong())

        unlockSatisfied(pid)
    }

    private suspend fun unlockSatisfied(pid: Long) {
        val snapshot = currentSnapshot(pid)
        val now = clock.nowMillis()
        AchievementCatalog.satisfied(snapshot).forEach { key ->
            achievementDao.insertIgnore(
                ProfileAchievementEntity(profileId = pid, key = key, unlockedAt = now)
            )
        }
    }

    private suspend fun currentSnapshot(pid: Long): GamificationSnapshot {
        val values = counterDao.getAll(pid).associate { it.key to it.value }
        val activeDays = countDao.activeDays(pid)
        val streak = Streaks.compute(activeDays, clock.todayEpochDay())
        return GamificationSnapshot(
            lifetimeTotal = countDao.lifetimeTotal(pid),
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
        val pid = profileId
        val last = counterDao.get(pid, lastDayKey)
        if (last == today) return
        counterDao.increment(pid, counterKey)
        counterDao.insertIgnore(ProfileCounterEntity(pid, lastDayKey, today))
        counterDao.setValue(pid, lastDayKey, today)
    }

    // Nothing here resets achievements or counters. A milestone, once reached, stays reached.
}
