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

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
