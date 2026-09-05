package com.lillah.dhikr.data.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.Migrations
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Bringing back counting that `INSERT OR REPLACE` destroyed.
 *
 * The damage is reproduced here with the statement that actually caused it, not with a stand-in:
 * the test issues a real `INSERT OR REPLACE INTO dhikr` against a real version 4 database with
 * foreign keys enabled, and the cascade takes the history with it exactly as it did on a phone.
 * Then the database is opened through the production migrations and the history has to be back.
 *
 * Versions 4 and 5 have byte-identical DDL and the same Room identity hash — 5 changes data, not
 * shape — so a v4 database is faithfully made by writing `user_version = 4`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationV4ToV5RepairTest {

    private val dbName = "repair-test.db"
    private val DEVICE = 1L
    private lateinit var context: Context
    private var database: DhikrDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(dbName)
    }

    private fun open(): DhikrDatabase =
        Room.databaseBuilder(context, DhikrDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    private fun close() {
        database?.close()
        database = null
    }

    /** One counted day, written the way the app writes it: ledger row and outbox row together. */
    private suspend fun DhikrDatabase.record(dhikrId: Long, epochDay: Long, delta: Int) {
        countDao().addCount(dhikrId, epochDay, delta, now = epochDay * 86_400_000L)
        syncDao().enqueue(
            SyncOperationEntity(
                opId = UUID.randomUUID().toString(),
                kind = "COUNT_DELTA",
                dhikrId = dhikrId,
                dhikrName = "Astaghfirullah",
                epochDay = epochDay,
                delta = delta.toLong(),
                createdAt = epochDay * 86_400_000L,
                state = "PENDING",
                profileId = DEVICE,
            )
        )
    }

    /** Rewinds an already-created database to version 4, which has the very same schema. */
    private fun rewindToV4() {
        val sqlite = android.database.sqlite.SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).absolutePath, null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
        )
        sqlite.execSQL("PRAGMA user_version = 4")
        sqlite.close()
    }

    /**
     * The statement the old DAO emitted, issued on the open Room connection.
     *
     * It has to be Room's own connection: SQLite implements `ON DELETE CASCADE` with internal
     * triggers, and a REPLACE only fires them when `recursive_triggers` is on — which is how
     * Android configures the connection Room uses, and is why this destroyed real data.
     */
    private fun DhikrDatabase.replaceDhikrRow(id: Long, newTarget: Int) {
        openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO dhikr (id, name, arabic, transliteration, meaning, virtue, " +
                "source, targetCount, dailyTarget, collectionId, sortOrder, accentIndex, " +
                "isFavorite, isArchived, isBuiltIn, currentCount, roundsToday, roundsEpochDay, " +
                "lastCountedAt, createdAt, profileId) VALUES " +
                "(?, 'Astaghfirullah', NULL, NULL, NULL, NULL, NULL, ?, NULL, NULL, 0, 0, " +
                "0, 0, 1, 0, 0, 0, NULL, 0, 1)",
            arrayOf<Any>(id, newTarget),
        )
    }

    /** A dhikr counted on three separate days: 40, 60 and 33. Leaves the database open. */
    private suspend fun DhikrDatabase.seedCountedDhikr(): Long {
        profileDao().insert(
            com.lillah.dhikr.data.local.entity.ProfileEntity(id = DEVICE, displayName = "This device")
        )
        val id = dhikrDao().upsert(
            DhikrEntity(name = "Astaghfirullah", targetCount = 33, createdAt = 0, profileId = DEVICE)
        )
        record(id, 20_150, 40)
        record(id, 20_151, 60)
        record(id, 20_153, 33)
        assertEquals(133L, countDao().lifetimeTotal(DEVICE))
        return id
    }

    @Test
    fun `history a REPLACE cascaded away is rebuilt from the ledger`() = runBlocking {
        val db = open()
        val id = db.seedCountedDhikr()

        // The bug, exactly as it happened: change repetitions per round.
        db.replaceDhikrRow(id, newTarget = 100)
        assertEquals(
            "the cascade must actually wipe, or this proves nothing",
            0L,
            db.countDao().lifetimeTotal(DEVICE),
        )
        close()
        rewindToV4()

        val repaired = open()
        assertEquals("every day comes back", 133L, repaired.countDao().lifetimeTotal(DEVICE))
        assertEquals(40, repaired.countDao().dayTotal(DEVICE, 20_150))
        assertEquals(60, repaired.countDao().dayTotal(DEVICE, 20_151))
        assertEquals(33, repaired.countDao().dayTotal(DEVICE, 20_153))
        assertEquals("the edit itself is kept", 100, repaired.dhikrDao().getById(id)!!.targetCount)
    }

    @Test
    fun `a day wiped and then counted on again is topped back up, not doubled`() = runBlocking {
        val db = open()
        val id = db.seedCountedDhikr()
        db.replaceDhikrRow(id, newTarget = 100)

        // The user carries on counting the same day without realising anything was lost.
        db.record(id, 20_153, 12)
        assertEquals(12L, db.countDao().lifetimeTotal(DEVICE))
        close()
        rewindToV4()

        val repaired = open()
        assertEquals(
            "the ledger holds 33 + 12 for that day, and that is what it must read",
            45,
            repaired.countDao().dayTotal(DEVICE, 20_153),
        )
        assertEquals(145L, repaired.countDao().lifetimeTotal(DEVICE))
    }

    @Test
    fun `a database that never hit the bug is left exactly as it was`() = runBlocking {
        open().seedCountedDhikr()
        close()
        rewindToV4()

        val repaired = open()
        assertEquals("nothing is added", 133L, repaired.countDao().lifetimeTotal(DEVICE))
        assertEquals(40, repaired.countDao().dayTotal(DEVICE, 20_150))
        assertEquals(60, repaired.countDao().dayTotal(DEVICE, 20_151))
        assertEquals(33, repaired.countDao().dayTotal(DEVICE, 20_153))
    }

    @Test
    fun `an undo recorded in the ledger is respected, not counted as an addition`() = runBlocking {
        val db = open()
        db.profileDao().insert(
            com.lillah.dhikr.data.local.entity.ProfileEntity(id = DEVICE, displayName = "This device")
        )
        val id = db.dhikrDao().upsert(
            DhikrEntity(name = "Astaghfirullah", targetCount = 33, createdAt = 0, profileId = DEVICE)
        )
        db.record(id, 20_150, 50)
        db.record(id, 20_150, -1)
        db.record(id, 20_150, -1)
        assertEquals(48L, db.countDao().lifetimeTotal(DEVICE))

        db.replaceDhikrRow(id, newTarget = 100)
        close()
        rewindToV4()

        val repaired = open()
        assertEquals("undone counts stay undone", 48, repaired.countDao().dayTotal(DEVICE, 20_150))
    }
}
