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
import com.lillah.dhikr.data.local.entity.AchievementEntity
import com.lillah.dhikr.data.local.entity.CollectionEntity
import com.lillah.dhikr.data.local.entity.CounterEntity
import com.lillah.dhikr.data.local.entity.DhikrCountEntity
import com.lillah.dhikr.data.local.entity.DhikrEntity

@Database(
    entities = [
        CollectionEntity::class,
        DhikrEntity::class,
        DhikrCountEntity::class,
        AchievementEntity::class,
        CounterEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DhikrDatabase : RoomDatabase() {

    abstract fun dhikrDao(): DhikrDao
    abstract fun collectionDao(): CollectionDao
    abstract fun countDao(): CountDao
    abstract fun achievementDao(): AchievementDao
    abstract fun counterDao(): CounterDao

    companion object {
        const val NAME = "dhikr.db"

        fun build(context: Context): DhikrDatabase =
            Room.databaseBuilder(context.applicationContext, DhikrDatabase::class.java, NAME)
                // Foreign keys drive the cascade that removes a dhikr's history with the dhikr.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
