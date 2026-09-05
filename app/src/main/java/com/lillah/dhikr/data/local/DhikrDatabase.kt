package com.lillah.dhikr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lillah.dhikr.data.local.dao.AchievementDao
import com.lillah.dhikr.data.local.dao.CollectionDao
import com.lillah.dhikr.data.local.dao.CounterDao
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.DhikrDao
import com.lillah.dhikr.data.local.dao.ProfileDao
import com.lillah.dhikr.data.local.dao.SyncDao
import com.lillah.dhikr.data.local.entity.AchievementEntity
import com.lillah.dhikr.data.local.entity.CollectionEntity
import com.lillah.dhikr.data.local.entity.CounterEntity
import com.lillah.dhikr.data.local.entity.DhikrCountEntity
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.data.local.entity.ProfileAchievementEntity
import com.lillah.dhikr.data.local.entity.ProfileCounterEntity
import com.lillah.dhikr.data.local.entity.ProfileEntity
import com.lillah.dhikr.data.local.entity.ProfileSyncStateEntity
import com.lillah.dhikr.data.local.entity.RemoteSnapshotEntity
import com.lillah.dhikr.data.local.entity.SyncOperationEntity

@Database(
    entities = [
        CollectionEntity::class,
        DhikrEntity::class,
        DhikrCountEntity::class,
        // Still registered, and still populated. The v2-to-v3 migration copied their rows into
        // the per-profile tables rather than moving them, so these remain as a permanent record.
        AchievementEntity::class,
        CounterEntity::class,
        SyncOperationEntity::class,
        RemoteSnapshotEntity::class,
        ProfileEntity::class,
        ProfileAchievementEntity::class,
        ProfileCounterEntity::class,
        ProfileSyncStateEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class DhikrDatabase : RoomDatabase() {

    abstract fun dhikrDao(): DhikrDao
    abstract fun collectionDao(): CollectionDao
    abstract fun countDao(): CountDao
    abstract fun achievementDao(): AchievementDao
    abstract fun counterDao(): CounterDao
    abstract fun syncDao(): SyncDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val NAME = "dhikr.db"

        fun build(context: Context): DhikrDatabase =
            Room.databaseBuilder(context.applicationContext, DhikrDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                // No fallbackToDestructiveMigration, deliberately and permanently. A missing
                // migration must fail loudly at open time; the alternative is Room quietly
                // dropping every table and handing the user an empty app.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
