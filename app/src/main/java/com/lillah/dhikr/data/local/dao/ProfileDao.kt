package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lillah.dhikr.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeById(id: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): ProfileEntity?

    /** The device profile: the one that holds anything counted before anyone signed in. */
    @Query("SELECT * FROM profiles WHERE id = 1")
    suspend fun deviceProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("UPDATE profiles SET lastActiveAt = :at WHERE id = :id")
    suspend fun touch(id: Long, at: Long)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    // No delete. Profiles, like everything else here, are only ever added.
}
