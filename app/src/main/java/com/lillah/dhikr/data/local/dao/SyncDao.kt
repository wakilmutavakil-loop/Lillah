package com.lillah.dhikr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lillah.dhikr.data.local.entity.RemoteSnapshotEntity
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    /**
     * IGNORE, not REPLACE. The baseline operation uses a deterministic id derived from the device,
     * so re-running the claim must be a no-op rather than resetting its state to pending and
     * pushing the same history a second time.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(operation: SyncOperationEntity): Long

    @Query(
        "SELECT * FROM sync_operations WHERE state != 'SYNCED' " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun pending(limit: Int = 200): List<SyncOperationEntity>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE state != 'SYNCED'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM sync_operations WHERE state != 'SYNCED'")
    fun observePendingTotal(): Flow<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM sync_operations WHERE opId = :opId)")
    suspend fun exists(opId: String): Boolean

    @Query(
        "UPDATE sync_operations SET state = 'SYNCED', ownerUid = :uid, " +
            "lastAttemptAt = :now, lastError = NULL WHERE opId IN (:opIds)"
    )
    suspend fun markSynced(opIds: List<String>, uid: String, now: Long)

    @Query(
        "UPDATE sync_operations SET state = 'PENDING', attempts = attempts + 1, " +
            "lastAttemptAt = :now, lastError = :error WHERE opId IN (:opIds)"
    )
    suspend fun markFailed(opIds: List<String>, error: String?, now: Long)

    /**
     * Everything the outbox has ever carried for ordinary counting, synced or not. Subtracting it
     * from the local lifetime total yields exactly the history that predates the outbox — the
     * amount a device upgrading from v1.0.0 needs to claim once.
     */
    @Query("SELECT COALESCE(SUM(delta), 0) FROM sync_operations WHERE kind = 'COUNT_DELTA'")
    suspend fun countDeltaTotal(): Long

    /**
     * Only ever called when the user explicitly asks to erase history. Sync state is never cleared
     * on sign-out — a signed-out device keeps its queue so nothing is lost if they sign back in.
     */
    @Query("DELETE FROM sync_operations")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSnapshot(snapshot: RemoteSnapshotEntity)

    @Query("SELECT * FROM remote_snapshot WHERE id = 0")
    fun observeSnapshot(): Flow<RemoteSnapshotEntity?>

    @Query("SELECT * FROM remote_snapshot WHERE id = 0")
    suspend fun snapshot(): RemoteSnapshotEntity?

    @Query("DELETE FROM remote_snapshot")
    suspend fun clearSnapshot()
}
