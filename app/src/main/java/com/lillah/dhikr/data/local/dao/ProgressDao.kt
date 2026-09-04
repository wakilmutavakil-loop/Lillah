package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lillah.dhikr.data.local.entity.ProfileAchievementEntity
import com.lillah.dhikr.data.local.entity.ProfileCounterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Achievements, per profile.
 *
 * Reads and writes `profile_achievements`. The original `achievements` table still exists with its
 * rows intact — the v2-to-v3 migration copied rather than moved them — and is never written to
 * again. Nothing in this app deletes either table.
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM profile_achievements WHERE profileId = :profileId")
    fun observeAll(profileId: Long): Flow<List<ProfileAchievementEntity>>

    @Query("SELECT * FROM profile_achievements WHERE profileId = :profileId")
    suspend fun getAll(profileId: Long): List<ProfileAchievementEntity>

    @Query(
        "SELECT * FROM profile_achievements WHERE profileId = :profileId AND celebrated = 0 " +
            "ORDER BY unlockedAt ASC"
    )
    fun observeUncelebrated(profileId: Long): Flow<List<ProfileAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ProfileAchievementEntity): Long

    @Query(
        "UPDATE profile_achievements SET celebrated = 1 " +
            "WHERE profileId = :profileId AND key = :key"
    )
    suspend fun markCelebrated(profileId: Long, key: String)
}

/** Counters, per profile. Same table split and the same permanence as achievements. */
@Dao
interface CounterDao {

    @Query("SELECT value FROM profile_counters WHERE profileId = :profileId AND key = :key")
    suspend fun get(profileId: Long, key: String): Long?

    @Query("SELECT * FROM profile_counters WHERE profileId = :profileId")
    fun observeAll(profileId: Long): Flow<List<ProfileCounterEntity>>

    @Query("SELECT * FROM profile_counters WHERE profileId = :profileId")
    suspend fun getAll(profileId: Long): List<ProfileCounterEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ProfileCounterEntity)

    @Query(
        "UPDATE profile_counters SET value = value + :delta " +
            "WHERE profileId = :profileId AND key = :key"
    )
    suspend fun addTo(profileId: Long, key: String, delta: Long)

    @Query(
        "UPDATE profile_counters SET value = :value WHERE profileId = :profileId AND key = :key"
    )
    suspend fun setValue(profileId: Long, key: String, value: Long)

    @Transaction
    suspend fun increment(profileId: Long, key: String, delta: Long = 1) {
        insertIgnore(ProfileCounterEntity(profileId, key, 0))
        addTo(profileId, key, delta)
    }

    /** Keeps a high-water mark such as "longest streak" or "best single session". */
    @Transaction
    suspend fun raiseTo(profileId: Long, key: String, value: Long) {
        insertIgnore(ProfileCounterEntity(profileId, key, 0))
        val current = get(profileId, key) ?: 0
        if (value > current) setValue(profileId, key, value)
    }
}
