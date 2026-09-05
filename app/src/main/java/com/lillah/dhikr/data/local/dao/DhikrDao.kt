package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
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

    /**
     * Inserts a new dhikr, or updates the existing row in place.
     *
     * **Never `OnConflictStrategy.REPLACE`.** SQLite implements `INSERT OR REPLACE` as a delete
     * followed by an insert, and `dhikr_counts` cascades on the delete of its dhikr — so saving
     * an edit through REPLACE erased every day that dhikr had ever been counted. `@Upsert`
     * inserts, and on a primary-key conflict issues an `UPDATE`, which touches no child row.
     */
    @Upsert
    suspend fun upsert(entity: DhikrEntity): Long

    /** Seeding only: adds shipped adhkar that are missing and leaves anything present alone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<DhikrEntity>): List<Long>

    /**
     * Changes the target alone.
     *
     * The counter's "Repetitions per round" sheet has no business rewriting the whole row: doing
     * that from a screen snapshot can roll back a live round if the user counted in between.
     */
    @Query("UPDATE dhikr SET targetCount = :target WHERE id = :id")
    suspend fun setTarget(id: Long, target: Int)

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
