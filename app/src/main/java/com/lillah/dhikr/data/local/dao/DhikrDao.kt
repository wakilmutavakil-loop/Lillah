package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lillah.dhikr.data.local.entity.DhikrEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrDao {

    @Query(
        "SELECT * FROM dhikr WHERE profileId = :profileId AND isArchived = 0 " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun observeActive(profileId: Long): Flow<List<DhikrEntity>>

    @Query(
        "SELECT * FROM dhikr WHERE profileId = :profileId AND isArchived = 1 " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun observeArchived(profileId: Long): Flow<List<DhikrEntity>>

    @Query(
        "SELECT * FROM dhikr WHERE profileId = :profileId AND isArchived = 0 AND isFavorite = 1 " +
            "ORDER BY sortOrder ASC"
    )
    fun observeFavorites(profileId: Long): Flow<List<DhikrEntity>>

    @Query(
        "SELECT * FROM dhikr WHERE collectionId = :collectionId AND isArchived = 0 " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun observeByCollection(collectionId: Long): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE id = :id")
    fun observeById(id: Long): Flow<DhikrEntity?>

    @Query("SELECT * FROM dhikr WHERE id = :id")
    suspend fun getById(id: Long): DhikrEntity?

    @Query("SELECT COUNT(*) FROM dhikr WHERE profileId = :profileId")
    suspend fun count(profileId: Long): Int

    @Query("SELECT COUNT(*) FROM dhikr")
    suspend fun countAllProfiles(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM dhikr WHERE profileId = :profileId")
    suspend fun maxSortOrder(profileId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DhikrEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DhikrEntity>): List<Long>

    @Update
    suspend fun update(entity: DhikrEntity)

    @Query("UPDATE dhikr SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE dhikr SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE dhikr SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun applyOrder(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query(
        "UPDATE dhikr SET currentCount = :currentCount, roundsToday = :roundsToday, " +
            "roundsEpochDay = :epochDay, lastCountedAt = :now WHERE id = :id"
    )
    suspend fun updateRoundState(
        id: Long,
        currentCount: Int,
        roundsToday: Int,
        epochDay: Long,
        now: Long,
    )

    @Query("UPDATE dhikr SET currentCount = 0 WHERE id = :id")
    suspend fun resetRound(id: Long)

    @Query("UPDATE dhikr SET dailyTarget = :dailyTarget WHERE id = :id")
    suspend fun setDailyTarget(id: Long, dailyTarget: Int?)

    @Query("SELECT name FROM dhikr WHERE collectionId = :collectionId")
    suspend fun namesInCollection(collectionId: Long): List<String>

    @Query("SELECT COUNT(*) FROM dhikr WHERE profileId = :profileId AND isBuiltIn = 0")
    suspend fun countCustom(profileId: Long): Int
}
