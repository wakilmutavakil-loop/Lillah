package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lillah.dhikr.data.local.entity.AchievementEntity
import com.lillah.dhikr.data.local.entity.CounterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE celebrated = 0 ORDER BY unlockedAt ASC")
    fun observeUncelebrated(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: AchievementEntity): Long

    @Query("UPDATE achievements SET celebrated = 1 WHERE key = :key")
    suspend fun markCelebrated(key: String)

    @Query("DELETE FROM achievements")
    suspend fun clearAll()
}

@Dao
interface CounterDao {

    @Query("SELECT value FROM counters WHERE key = :key")
    suspend fun get(key: String): Long?

    @Query("SELECT * FROM counters")
    fun observeAll(): Flow<List<CounterEntity>>

    @Query("SELECT * FROM counters")
    suspend fun getAll(): List<CounterEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: CounterEntity)

    @Query("UPDATE counters SET value = value + :delta WHERE key = :key")
    suspend fun addTo(key: String, delta: Long)

    @Query("UPDATE counters SET value = :value WHERE key = :key")
    suspend fun setValue(key: String, value: Long)

    @Transaction
    suspend fun increment(key: String, delta: Long = 1) {
        insertIgnore(CounterEntity(key, 0))
        addTo(key, delta)
    }

    /** Keeps a high-water mark such as "longest streak" or "best single session". */
    @Transaction
    suspend fun raiseTo(key: String, value: Long) {
        insertIgnore(CounterEntity(key, 0))
        val current = get(key) ?: 0
        if (value > current) setValue(key, value)
    }

    @Query("DELETE FROM counters")
    suspend fun clearAll()
}
