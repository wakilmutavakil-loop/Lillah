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
import com.lillah.dhikr.data.local.dao.SyncDao
import com.lillah.dhikr.data.local.entity.AchievementEntity
import com.lillah.dhikr.data.local.entity.CollectionEntity
import com.lillah.dhikr.data.local.entity.CounterEntity
import com.lillah.dhikr.data.local.entity.DhikrCountEntity
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.data.local.entity.RemoteSnapshotEntity
import com.lillah.dhikr.data.local.entity.SyncOperationEntity

@Database(
    entities = [
        CollectionEntity::class,
        DhikrEntity::class,
        DhikrCountEntity::class,
        AchievementEntity::class,
        CounterEntity::class,
        SyncOperationEntity::class,
        RemoteSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class DhikrDatabase : RoomDatabase() {

    abstract fun dhikrDao(): DhikrDao
    abstract fun collectionDao(): CollectionDao
    abstract fun countDao(): CountDao
    abstract fun achievementDao(): AchievementDao
    abstract fun counterDao(): CounterDao
    abstract fun syncDao(): SyncDao

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
