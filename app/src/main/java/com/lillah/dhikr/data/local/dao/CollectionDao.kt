package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.lillah.dhikr.data.local.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query(
        "SELECT * FROM collections WHERE profileId = :profileId AND isArchived = 0 " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun observeAll(profileId: Long): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeById(id: Long): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: Long): CollectionEntity?

    @Query("SELECT * FROM collections WHERE profileId = :profileId AND kind = :kind LIMIT 1")
    suspend fun getByKind(profileId: Long, kind: String): CollectionEntity?

    @Query("SELECT COUNT(*) FROM collections WHERE profileId = :profileId")
    suspend fun count(profileId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM collections WHERE profileId = :profileId")
    suspend fun maxSortOrder(profileId: Long): Int

    /**
     * Inserts a new collection, or updates the existing row in place.
     *
     * Never REPLACE: `dhikr.collectionId` is `ON DELETE SET NULL`, and REPLACE deletes before it
     * inserts — so renaming a collection emptied it of every dhikr it held.
     */
    @Upsert
    suspend fun upsert(entity: CollectionEntity): Long

    @Update
    suspend fun update(entity: CollectionEntity)

    @Query("UPDATE collections SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE collections SET coverImagePath = :path WHERE id = :id")
    suspend fun setCoverImage(id: Long, path: String?)

    @Query("SELECT coverImagePath FROM collections WHERE coverImagePath IS NOT NULL")
    suspend fun coverImagePaths(): List<String>

}
