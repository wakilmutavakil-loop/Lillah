package com.lillah.dhikr.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val arabicName: String? = null,
    val description: String? = null,
    /** Matches a [com.lillah.dhikr.domain.model.CollectionKind] name. */
    val kind: String,
    /** Key of the built-in vector artwork drawn when no user cover has been chosen. */
    val artworkKey: String,
    /** Absolute path of a user-supplied cover copied into app storage, if any. */
    val coverImagePath: String? = null,
    val accentIndex: Int = 0,
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = false,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "dhikr",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("collectionId"), Index("sortOrder"), Index("isArchived")],
)
data class DhikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val arabic: String? = null,
    val transliteration: String? = null,
    val meaning: String? = null,
    /** Short note on the reported merit or occasion. Shown beneath the counter. */
    val virtue: String? = null,
    val source: String? = null,
    /** Repetitions in one round. */
    val targetCount: Int = 33,
    /** Optional personal goal for the whole day; null means "no daily goal". */
    val dailyTarget: Int? = null,
    val collectionId: Long? = null,
    val sortOrder: Int = 0,
    val accentIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isBuiltIn: Boolean = false,
    /** Live round progress, persisted on every tap so a round survives being killed. */
    val currentCount: Int = 0,
    val roundsToday: Int = 0,
    val roundsEpochDay: Long = 0,
    val lastCountedAt: Long? = null,
    val createdAt: Long = 0,
)

/**
 * One row per dhikr per day. Compact enough to keep forever, and rich enough to answer every
 * question the Progress screen asks: today's totals, weekly bars, monthly heatmap, lifetime sum.
 */
@Entity(
    tableName = "dhikr_counts",
    primaryKeys = ["dhikrId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = DhikrEntity::class,
            parentColumns = ["id"],
            childColumns = ["dhikrId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("epochDay")],
)
data class DhikrCountEntity(
    val dhikrId: Long,
    val epochDay: Long,
    val count: Int,
    val updatedAt: Long,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val unlockedAt: Long,
    val celebrated: Boolean = false,
)

/** Small key/value ledger for figures that cannot be re-derived from the count history. */
@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey val key: String,
    val value: Long,
)

/**
 * The outbox. Every count produces one append-only row here, written in the same transaction as
 * the count itself, so a contribution can never be recorded locally without also being queued for
 * the cloud.
 *
 * [opId] is a client-generated UUID that doubles as the remote document id. Pushing the same
 * operation twice therefore writes the same document twice, which the backend folds into the
 * totals exactly once — retries after a timeout or a crash cannot double count.
 *
 * Deliberately carries no foreign key to `dhikr`: deleting a dhikr must not retract a
 * contribution that has already been made, so [dhikrName] is denormalised here.
 */
@Entity(tableName = "sync_operations", indices = [Index("state"), Index("createdAt")])
data class SyncOperationEntity(
    @PrimaryKey val opId: String,
    /** Matches a [com.lillah.dhikr.domain.sync.SyncOperationKind] name. */
    val kind: String,
    val dhikrId: Long? = null,
    val dhikrName: String? = null,
    val epochDay: Long,
    /** Long, not Int: a baseline claim carries a whole history in one operation. */
    val delta: Long,
    val createdAt: Long,
    /** Matches a [com.lillah.dhikr.domain.sync.SyncState] name. */
    val state: String,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    /** The account this operation was accepted under, once it has been. */
    val ownerUid: String? = null,
)

/**
 * Last known cloud figures, cached so the Universal Dhikr board still renders something truthful
 * when the device is offline. Single row, always id 0.
 */
@Entity(tableName = "remote_snapshot")
data class RemoteSnapshotEntity(
    @PrimaryKey val id: Int = 0,
    val globalTotal: Long = 0,
    val globalToday: Long = 0,
    val participantCount: Long = 0,
    val userTotal: Long = 0,
    val userUid: String? = null,
    val updatedAt: Long = 0,
)
