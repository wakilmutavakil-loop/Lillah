package com.lillah.dhikr.data.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

/**
 * Builds a genuine version 1 database — the exact DDL Room generated for the released v1.0.0 app,
 * taken verbatim from `app/schemas/.../1.json`, including the identity hash Room checks on open.
 *
 * Migration tests are only worth anything if they run against the real old schema. Recreating it
 * by hand here means the tests fail if a migration stops lining up with what is actually installed
 * on somebody's phone.
 */
object V1DatabaseBuilder {

    /** From `1.json`. Room compares this on open and refuses to continue if it disagrees. */
    const val V1_IDENTITY_HASH = "100ae6643aeb1ecabadc7f45e99b1c2e"

    private val V1_TABLES = listOf(
        "CREATE TABLE IF NOT EXISTS `collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, `arabicName` TEXT, `description` TEXT, `kind` TEXT NOT NULL, " +
            "`artworkKey` TEXT NOT NULL, `coverImagePath` TEXT, `accentIndex` INTEGER NOT NULL, " +
            "`sortOrder` INTEGER NOT NULL, `isBuiltIn` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)",

        "CREATE TABLE IF NOT EXISTS `dhikr` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, `arabic` TEXT, `transliteration` TEXT, `meaning` TEXT, " +
            "`virtue` TEXT, `source` TEXT, `targetCount` INTEGER NOT NULL, `dailyTarget` INTEGER, " +
            "`collectionId` INTEGER, `sortOrder` INTEGER NOT NULL, `accentIndex` INTEGER NOT NULL, " +
            "`isFavorite` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isBuiltIn` INTEGER NOT NULL, " +
            "`currentCount` INTEGER NOT NULL, `roundsToday` INTEGER NOT NULL, " +
            "`roundsEpochDay` INTEGER NOT NULL, `lastCountedAt` INTEGER, `createdAt` INTEGER NOT NULL, " +
            "FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE SET NULL )",
        "CREATE INDEX IF NOT EXISTS `index_dhikr_collectionId` ON `dhikr` (`collectionId`)",
        "CREATE INDEX IF NOT EXISTS `index_dhikr_sortOrder` ON `dhikr` (`sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `index_dhikr_isArchived` ON `dhikr` (`isArchived`)",

        "CREATE TABLE IF NOT EXISTS `dhikr_counts` (`dhikrId` INTEGER NOT NULL, " +
            "`epochDay` INTEGER NOT NULL, `count` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`dhikrId`, `epochDay`), FOREIGN KEY(`dhikrId`) REFERENCES `dhikr`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_dhikr_counts_epochDay` ON `dhikr_counts` (`epochDay`)",

        "CREATE TABLE IF NOT EXISTS `achievements` (`key` TEXT NOT NULL, " +
            "`unlockedAt` INTEGER NOT NULL, `celebrated` INTEGER NOT NULL, PRIMARY KEY(`key`))",

        "CREATE TABLE IF NOT EXISTS `counters` (`key` TEXT NOT NULL, `value` INTEGER NOT NULL, " +
            "PRIMARY KEY(`key`))",

        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
            "VALUES(42, '$V1_IDENTITY_HASH')",
    )

    /** Shared with [V2DatabaseBuilder], which is v1's schema plus the sync tables. */
    fun v1Tables(): List<String> = V1_TABLES

    /** Opens (creating if needed) a real v1 database file and hands it to [populate]. */
    fun create(
        context: Context,
        name: String,
        populate: (SupportSQLiteDatabase) -> Unit = {},
    ) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    V1_TABLES.forEach(db::execSQL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase.use { db ->
            check(db.version == 1) { "expected a version 1 database, got ${db.version}" }
            populate(db)
        }
    }

    fun insertCollection(db: SupportSQLiteDatabase, id: Long, name: String) {
        db.execSQL(
            "INSERT INTO collections (id, name, arabicName, description, kind, artworkKey, " +
                "coverImagePath, accentIndex, sortOrder, isBuiltIn, isArchived) " +
                "VALUES (?, ?, ?, ?, 'Morning', 'Sunrise', NULL, 3, 0, 1, 0)",
            arrayOf(id, name, "أذكار الصباح", "Said after Fajr"),
        )
    }

    @Suppress("LongParameterList")
    fun insertDhikr(
        db: SupportSQLiteDatabase,
        id: Long,
        name: String,
        collectionId: Long?,
        targetCount: Int = 33,
        currentCount: Int = 0,
        isFavorite: Boolean = false,
        arabic: String? = "سُبْحَانَ اللَّهِ",
    ) {
        db.execSQL(
            "INSERT INTO dhikr (id, name, arabic, transliteration, meaning, virtue, source, " +
                "targetCount, dailyTarget, collectionId, sortOrder, accentIndex, isFavorite, " +
                "isArchived, isBuiltIn, currentCount, roundsToday, roundsEpochDay, lastCountedAt, " +
                "createdAt) VALUES (?, ?, ?, 'SubhanAllah', 'Glory be to Allah', NULL, NULL, " +
                "?, NULL, ?, 0, 2, ?, 0, 1, ?, 4, 20000, 1700000000000, 1690000000000)",
            arrayOf(
                id, name, arabic, targetCount, collectionId,
                if (isFavorite) 1 else 0, currentCount,
            ),
        )
    }

    fun insertCount(db: SupportSQLiteDatabase, dhikrId: Long, epochDay: Long, count: Int) {
        db.execSQL(
            "INSERT INTO dhikr_counts (dhikrId, epochDay, count, updatedAt) VALUES (?, ?, ?, ?)",
            arrayOf(dhikrId, epochDay, count, 1700000000000L),
        )
    }

    fun insertAchievement(db: SupportSQLiteDatabase, key: String) {
        db.execSQL(
            "INSERT INTO achievements (key, unlockedAt, celebrated) VALUES (?, 1700000000000, 1)",
            arrayOf(key),
        )
    }

    fun insertCounter(db: SupportSQLiteDatabase, key: String, value: Long) {
        db.execSQL("INSERT INTO counters (key, value) VALUES (?, ?)", arrayOf(key, value))
    }
}

/**
 * A genuine version 2 database — the schema shipped in v1.1.0, taken verbatim from `2.json`.
 *
 * Covers the other upgrade path into v1.2.0: someone already running v1.1.0, whose database has
 * the sync tables but no profiles.
 */
object V2DatabaseBuilder {

    const val V2_IDENTITY_HASH = "3d108bcc5a45d79fd5de72226f5c009c"

    private val V2_EXTRA_TABLES = listOf(
        "CREATE TABLE IF NOT EXISTS `sync_operations` (`opId` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
            "`dhikrId` INTEGER, `dhikrName` TEXT, `epochDay` INTEGER NOT NULL, " +
            "`delta` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `state` TEXT NOT NULL, " +
            "`attempts` INTEGER NOT NULL, `lastAttemptAt` INTEGER, `lastError` TEXT, " +
            "`ownerUid` TEXT, PRIMARY KEY(`opId`))",
        "CREATE INDEX IF NOT EXISTS `index_sync_operations_state` ON `sync_operations` (`state`)",
        "CREATE INDEX IF NOT EXISTS `index_sync_operations_createdAt` " +
            "ON `sync_operations` (`createdAt`)",
        "CREATE TABLE IF NOT EXISTS `remote_snapshot` (`id` INTEGER NOT NULL, " +
            "`globalTotal` INTEGER NOT NULL, `globalToday` INTEGER NOT NULL, " +
            "`participantCount` INTEGER NOT NULL, `userTotal` INTEGER NOT NULL, " +
            "`userUid` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    )

    fun create(
        context: Context,
        name: String,
        populate: (SupportSQLiteDatabase) -> Unit = {},
    ) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // v2 is v1 plus the sync tables, so reuse v1's DDL and add to it.
                    V1DatabaseBuilder.v1Tables().forEach(db::execSQL)
                    V2_EXTRA_TABLES.forEach(db::execSQL)
                    db.execSQL(
                        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                            "VALUES(42, '$V2_IDENTITY_HASH')"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase.use { db ->
            check(db.version == 2) { "expected a version 2 database, got ${db.version}" }
            populate(db)
        }
    }

    fun insertOperation(db: SupportSQLiteDatabase, opId: String, delta: Long, synced: Boolean) {
        db.execSQL(
            "INSERT INTO sync_operations (opId, kind, dhikrId, dhikrName, epochDay, delta, " +
                "createdAt, state, attempts, lastAttemptAt, lastError, ownerUid) " +
                "VALUES (?, 'COUNT_DELTA', 1, 'SubhanAllah', 19000, ?, 0, ?, 0, NULL, NULL, NULL)",
            arrayOf(opId, delta, if (synced) "SYNCED" else "PENDING"),
        )
    }
}
