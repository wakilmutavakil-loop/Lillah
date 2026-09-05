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

    /**
     * 4 to 5 — repairing history destroyed by `INSERT OR REPLACE`.
     *
     * Saving an edit to a dhikr went through `INSERT OR REPLACE`, and SQLite implements REPLACE
     * as a *delete followed by an insert*. `dhikr_counts` cascades on the delete of its dhikr, so
     * every edit — changing "repetitions per round" above all, which is a one-tap action —
     * silently erased that dhikr's entire counting history. Version 5 of the code cannot do that
     * any more. This migration is about the damage already done.
     *
     * The history is recoverable because `sync_operations` is an append-only ledger that records
     * every single count as `(dhikrId, epochDay, delta)`, is written in the same transaction as
     * the count itself, is never deleted from, and — deliberately — carries no foreign key to
     * `dhikr`. The cascade could not touch it. Summing it by day therefore reconstructs exactly
     * what `dhikr_counts` held.
     *
     * Two statements, both strictly additive:
     *
     *  1. put back the days that are missing outright, and
     *  2. raise any day that survived but is *lower* than the ledger — which is what a user sees
     *     after being wiped and then carrying on counting on the same day.
     *
     * Nothing is ever lowered or removed. A device that never hit the bug has a ledger that
     * agrees with its counts, so both statements find nothing to do and the database is untouched.
     *
     * Raising a surviving row is only sound if the ledger cannot legitimately exceed the counts,
     * and it cannot: every `COUNT_DELTA` row is written by `increment` or `decrement` inside the
     * same transaction as its `addCount`, with the same dhikr, day and delta, and those are the
     * only two places that enqueue one. `BASELINE` is filtered out here and is in any case never
     * produced. The one way the two can drift is `addCount`'s `MAX(0, …)` clamp, which can leave
     * the ledger *lower* after an undo at zero — the direction this migration ignores. So a day
     * whose ledger exceeds its count is a day that lost rows, and the cascade is the only thing
     * that removes them.
     *
     * One honest limit: the ledger only exists from v1.1.0, when the outbox was added. Counting
     * erased before that upgrade has no record anywhere and cannot be brought back.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Days the cascade removed entirely.
            db.execSQL(
                """
                INSERT INTO `dhikr_counts` (`dhikrId`, `epochDay`, `count`, `updatedAt`)
                SELECT o.`dhikrId`, o.`epochDay`, SUM(o.`delta`), MAX(o.`createdAt`)
                FROM `sync_operations` o
                WHERE o.`kind` = 'COUNT_DELTA'
                  AND o.`dhikrId` IS NOT NULL
                  AND EXISTS (SELECT 1 FROM `dhikr` d WHERE d.`id` = o.`dhikrId`)
                  AND NOT EXISTS (
                      SELECT 1 FROM `dhikr_counts` c
                      WHERE c.`dhikrId` = o.`dhikrId` AND c.`epochDay` = o.`epochDay`
                  )
                GROUP BY o.`dhikrId`, o.`epochDay`
                HAVING SUM(o.`delta`) > 0
                """.trimIndent()
            )

            // Days wiped and then counted on again, which survive holding only the remainder.
            db.execSQL(
                """
                UPDATE `dhikr_counts` SET `count` = (
                    SELECT SUM(o.`delta`) FROM `sync_operations` o
                    WHERE o.`kind` = 'COUNT_DELTA'
                      AND o.`dhikrId` = `dhikr_counts`.`dhikrId`
                      AND o.`epochDay` = `dhikr_counts`.`epochDay`
                )
                WHERE (
                    SELECT SUM(o.`delta`) FROM `sync_operations` o
                    WHERE o.`kind` = 'COUNT_DELTA'
                      AND o.`dhikrId` = `dhikr_counts`.`dhikrId`
                      AND o.`epochDay` = `dhikr_counts`.`epochDay`
                ) > `count`
                """.trimIndent()
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
