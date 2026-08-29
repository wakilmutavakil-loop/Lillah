package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lillah.dhikr.data.local.entity.DhikrCountEntity
import kotlinx.coroutines.flow.Flow

data class DayTotal(val epochDay: Long, val total: Int)

/** How far through a collection the user is on a given day. */
data class CollectionCompletion(
    val collectionId: Long,
    val itemCount: Int,
    val completedCount: Int,
    val totalToday: Int,
)

data class DhikrDayTotal(
    val dhikrId: Long,
    val name: String,
    val arabic: String?,
    val accentIndex: Int,
    val targetCount: Int,
    val total: Int,
)

@Dao
interface CountDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: DhikrCountEntity)

    @Query(
        "UPDATE dhikr_counts SET count = MAX(0, count + :delta), updatedAt = :now " +
            "WHERE dhikrId = :dhikrId AND epochDay = :epochDay"
    )
    suspend fun addToExisting(dhikrId: Long, epochDay: Long, delta: Int, now: Long)

    /**
     * Insert-then-update rather than `ON CONFLICT DO UPDATE`: SQLite only gained UPSERT in 3.24,
     * which lands on Android 11, and this app supports Android 8.
     */
    @Transaction
    suspend fun addCount(dhikrId: Long, epochDay: Long, delta: Int, now: Long) {
        insertIgnore(DhikrCountEntity(dhikrId, epochDay, 0, now))
        addToExisting(dhikrId, epochDay, delta, now)
    }

    @Query("SELECT COALESCE(SUM(count), 0) FROM dhikr_counts WHERE epochDay = :epochDay")
    fun observeDayTotal(epochDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM dhikr_counts")
    fun observeLifetimeTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(count), 0) FROM dhikr_counts")
    suspend fun lifetimeTotal(): Long

    @Query(
        "SELECT COALESCE(count, 0) FROM dhikr_counts WHERE dhikrId = :dhikrId AND epochDay = :epochDay"
    )
    fun observeDhikrDayCount(dhikrId: Long, epochDay: Long): Flow<Int?>

    @Query(
        "SELECT epochDay AS epochDay, SUM(count) AS total FROM dhikr_counts " +
            "WHERE epochDay BETWEEN :fromDay AND :toDay GROUP BY epochDay ORDER BY epochDay ASC"
    )
    fun observeDayTotals(fromDay: Long, toDay: Long): Flow<List<DayTotal>>

    @Query(
        "SELECT epochDay AS epochDay, SUM(count) AS total FROM dhikr_counts " +
            "WHERE epochDay BETWEEN :fromDay AND :toDay GROUP BY epochDay ORDER BY epochDay ASC"
    )
    suspend fun dayTotals(fromDay: Long, toDay: Long): List<DayTotal>

    @Query(
        "SELECT c.dhikrId AS dhikrId, d.name AS name, d.arabic AS arabic, " +
            "d.accentIndex AS accentIndex, d.targetCount AS targetCount, c.count AS total " +
            "FROM dhikr_counts c INNER JOIN dhikr d ON d.id = c.dhikrId " +
            "WHERE c.epochDay = :epochDay AND c.count > 0 ORDER BY c.count DESC"
    )
    fun observeDayBreakdown(epochDay: Long): Flow<List<DhikrDayTotal>>

    @Query(
        "SELECT c.dhikrId AS dhikrId, d.name AS name, d.arabic AS arabic, " +
            "d.accentIndex AS accentIndex, d.targetCount AS targetCount, SUM(c.count) AS total " +
            "FROM dhikr_counts c INNER JOIN dhikr d ON d.id = c.dhikrId " +
            "WHERE c.epochDay BETWEEN :fromDay AND :toDay AND c.count > 0 " +
            "GROUP BY c.dhikrId ORDER BY total DESC"
    )
    fun observeRangeBreakdown(fromDay: Long, toDay: Long): Flow<List<DhikrDayTotal>>

    /** Days on which anything at all was counted — the raw material for streaks. */
    @Query("SELECT DISTINCT epochDay FROM dhikr_counts WHERE count > 0 ORDER BY epochDay DESC")
    fun observeActiveDays(): Flow<List<Long>>

    @Query("SELECT DISTINCT epochDay FROM dhikr_counts WHERE count > 0 ORDER BY epochDay DESC")
    suspend fun activeDays(): List<Long>

    @Query("SELECT COUNT(DISTINCT epochDay) FROM dhikr_counts WHERE count > 0")
    fun observeActiveDayCount(): Flow<Int>

    @Query("SELECT MIN(epochDay) FROM dhikr_counts WHERE count > 0")
    fun observeFirstActiveDay(): Flow<Long?>

    @Query(
        "SELECT COALESCE(SUM(count), 0) FROM dhikr_counts " +
            "WHERE dhikrId IN (SELECT id FROM dhikr WHERE collectionId = :collectionId) " +
            "AND epochDay = :epochDay"
    )
    fun observeCollectionDayTotal(collectionId: Long, epochDay: Long): Flow<Int>

    @Query(
        "SELECT d.collectionId AS collectionId, COUNT(*) AS itemCount, " +
            "SUM(CASE WHEN COALESCE(c.count, 0) >= d.targetCount THEN 1 ELSE 0 END) AS completedCount, " +
            "COALESCE(SUM(COALESCE(c.count, 0)), 0) AS totalToday " +
            "FROM dhikr d LEFT JOIN dhikr_counts c " +
            "ON c.dhikrId = d.id AND c.epochDay = :epochDay " +
            "WHERE d.isArchived = 0 AND d.collectionId IS NOT NULL " +
            "GROUP BY d.collectionId"
    )
    fun observeCollectionCompletions(epochDay: Long): Flow<List<CollectionCompletion>>

    @Query(
        "SELECT d.collectionId AS collectionId, COUNT(*) AS itemCount, " +
            "SUM(CASE WHEN COALESCE(c.count, 0) >= d.targetCount THEN 1 ELSE 0 END) AS completedCount, " +
            "COALESCE(SUM(COALESCE(c.count, 0)), 0) AS totalToday " +
            "FROM dhikr d LEFT JOIN dhikr_counts c " +
            "ON c.dhikrId = d.id AND c.epochDay = :epochDay " +
            "WHERE d.isArchived = 0 AND d.collectionId = :collectionId " +
            "GROUP BY d.collectionId"
    )
    suspend fun collectionCompletion(collectionId: Long, epochDay: Long): CollectionCompletion?

    @Query("SELECT COALESCE(SUM(count), 0) FROM dhikr_counts WHERE epochDay = :epochDay")
    suspend fun dayTotal(epochDay: Long): Int

    @Query("DELETE FROM dhikr_counts WHERE epochDay = :epochDay")
    suspend fun clearDay(epochDay: Long)

    @Query("DELETE FROM dhikr_counts")
    suspend fun clearAll()
}
