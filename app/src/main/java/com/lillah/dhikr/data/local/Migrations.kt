package com.lillah.dhikr.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * Every migration here is additive: `CREATE TABLE` and `ALTER TABLE ... ADD COLUMN` only. Nothing
 * drops, renames, recreates or rewrites a table that holds user data, and the database is built
 * without `fallbackToDestructiveMigration`, so a migration gap fails loudly at open time rather
 * than silently deleting somebody's history.
 */
object Migrations {

    /**
     * 1 to 2 — cloud sync.
     *
     * Adds the operation outbox and the cached remote snapshot. No existing table is touched, so
     * every dhikr, every daily count, every achievement and every counter carries over untouched
     * by construction rather than by careful copying.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_operations` (
                    `opId` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `dhikrId` INTEGER,
                    `dhikrName` TEXT,
                    `epochDay` INTEGER NOT NULL,
                    `delta` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `state` TEXT NOT NULL,
                    `attempts` INTEGER NOT NULL,
                    `lastAttemptAt` INTEGER,
                    `lastError` TEXT,
                    `ownerUid` TEXT,
                    PRIMARY KEY(`opId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_operations_state` " +
                    "ON `sync_operations` (`state`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_operations_createdAt` " +
                    "ON `sync_operations` (`createdAt`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `remote_snapshot` (
                    `id` INTEGER NOT NULL,
                    `globalTotal` INTEGER NOT NULL,
                    `globalToday` INTEGER NOT NULL,
                    `participantCount` INTEGER NOT NULL,
                    `userTotal` INTEGER NOT NULL,
                    `userUid` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    /**
     * 2 to 3 — one device, several people.
     *
     * Adds the profiles table and a `profileId` on everything a person owns, defaulting to 1: the
     * device profile, which is created here and given every row that already exists. Nobody's
     * history moves, because it all already belongs to the only profile there is.
     *
     * Achievements and counters need a composite key, and SQLite cannot add one to an existing
     * table without rebuilding it. Rather than rebuild a table holding user data, their rows are
     * copied into new tables and the originals are left exactly where they are — still populated,
     * never dropped, available if anything ever needs to be reconstructed from them.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `uid` TEXT,
                    `displayName` TEXT,
                    `email` TEXT,
                    `photoUrl` TEXT,
                    `method` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `lastActiveAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_profiles_uid` ON `profiles` (`uid`)"
            )

            val now = System.currentTimeMillis()
            // Explicit id 1: every existing row defaults to profileId 1, so the device profile
            // must be that row for the defaults to point at something real.
            db.execSQL(
                "INSERT OR IGNORE INTO `profiles` " +
                    "(`id`, `uid`, `displayName`, `email`, `photoUrl`, `method`, " +
                    "`createdAt`, `lastActiveAt`) " +
                    "VALUES (1, NULL, 'This device', NULL, NULL, NULL, ?, ?)",
                arrayOf(now, now),
            )

            db.execSQL("ALTER TABLE `dhikr` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
            db.execSQL(
                "ALTER TABLE `collections` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL(
                "ALTER TABLE `sync_operations` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_dhikr_profileId` ON `dhikr` (`profileId`)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_collections_profileId` " +
                    "ON `collections` (`profileId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_operations_profileId` " +
                    "ON `sync_operations` (`profileId`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `profile_achievements` (
                    `profileId` INTEGER NOT NULL,
                    `key` TEXT NOT NULL,
                    `unlockedAt` INTEGER NOT NULL,
                    `celebrated` INTEGER NOT NULL,
                    PRIMARY KEY(`profileId`, `key`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT OR IGNORE INTO `profile_achievements` " +
                    "(`profileId`, `key`, `unlockedAt`, `celebrated`) " +
                    "SELECT 1, `key`, `unlockedAt`, `celebrated` FROM `achievements`"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `profile_counters` (
                    `profileId` INTEGER NOT NULL,
                    `key` TEXT NOT NULL,
                    `value` INTEGER NOT NULL,
                    PRIMARY KEY(`profileId`, `key`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT OR IGNORE INTO `profile_counters` (`profileId`, `key`, `value`) " +
                    "SELECT 1, `key`, `value` FROM `counters`"
            )
        }
    }

    /**
     * 3 to 4 — one write per connect.
     *
     * Sync used to upload one document per counted tap. It now uploads a single running total,
     * which needs somewhere to remember the figure the server last accepted. That is all this
     * migration adds: one new table, nothing altered, nothing removed. The old
     * `sync_operations` rows stay exactly where they are and are still written on every count as
     * the device's own record of what happened.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `profile_sync_state` (
                    `profileId` INTEGER NOT NULL,
                    `lastUploadedTotal` INTEGER NOT NULL,
                    `lastUploadedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`profileId`)
                )
                """.trimIndent()
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
