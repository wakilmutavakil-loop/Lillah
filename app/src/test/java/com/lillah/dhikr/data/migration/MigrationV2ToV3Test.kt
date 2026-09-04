package com.lillah.dhikr.data.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.Migrations
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The other upgrade path: someone already on v1.1.0, whose database has the sync tables but no
 * profiles. Everything they own has to land in the device profile without a single row moving.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationV2ToV3Test {

    private val dbName = "migration-v2-test.db"
    private val device = 1L
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

    private fun openUpgraded(): DhikrDatabase =
        Room.databaseBuilder(context, DhikrDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    @Test
    fun `a v2 database keeps its counts, achievements and queue`() = runBlocking {
        V2DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertCollection(db, 1, "Morning Adhkar")
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", 1, currentCount = 9, isFavorite = true)
            V1DatabaseBuilder.insertCount(db, 1, 19_000, 18_000)
            V1DatabaseBuilder.insertCount(db, 1, 19_001, 7_000)
            V1DatabaseBuilder.insertAchievement(db, "thousand")
            V1DatabaseBuilder.insertCounter(db, "best_day", 1_200)
            V2DatabaseBuilder.insertOperation(db, "op-synced", 18_000, synced = true)
            V2DatabaseBuilder.insertOperation(db, "op-pending", 7_000, synced = false)
        }

        val db = openUpgraded()

        assertEquals(25_000L, db.countDao().lifetimeTotal(device))
        assertEquals(1, db.dhikrDao().count(device))
        assertEquals(1, db.collectionDao().count(device))

        val dhikr = db.dhikrDao().getById(1)
        assertNotNull(dhikr)
        assertEquals(device, dhikr!!.profileId)
        assertEquals(9, dhikr.currentCount)
        assertTrue(dhikr.isFavorite)

        // Achievements and counters were copied into the per-profile tables.
        assertEquals(1, db.achievementDao().getAll(device).size)
        assertEquals(1_200L, db.counterDao().get(device, "best_day"))

        // The queue survives, and knows which profile it belongs to.
        val pending = db.syncDao().pending(device)
        assertEquals(1, pending.size)
        assertEquals(7_000L, pending.single().delta)
        assertEquals(device, pending.single().profileId)
        assertEquals(25_000L, db.syncDao().countDeltaTotal(device))
    }

    @Test
    fun `the migration creates the device profile that everything defaults to`() = runBlocking {
        V2DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
        }

        val db = openUpgraded()
        val profile = db.profileDao().deviceProfile()

        assertNotNull(profile)
        assertEquals(1L, profile!!.id)
        assertNull("nobody has claimed it yet", profile.uid)
        assertEquals(1, db.profileDao().count())
    }

    @Test
    fun `the original achievement and counter tables are left populated`() = runBlocking {
        V2DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
            V1DatabaseBuilder.insertAchievement(db, "first_remembrance")
            V1DatabaseBuilder.insertCounter(db, "goal_days", 44)
        }

        openUpgraded()

        // Copied, not moved. The pre-v3 tables are never written to again and never dropped.
        val raw = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        raw.use { db ->
            db.rawQuery("SELECT COUNT(*) FROM achievements", null).use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            db.rawQuery("SELECT value FROM counters WHERE key = 'goal_days'", null).use {
                it.moveToFirst()
                assertEquals(44, it.getInt(0))
            }
        }
    }

    @Test
    fun `the sync state table arrives empty, so an unsynced device owes its whole history`() =
        runBlocking {
            V2DatabaseBuilder.create(context, dbName) { db ->
                V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
                V1DatabaseBuilder.insertCount(db, 1, 19_000, 25_000)
            }

            val db = openUpgraded()

            // Nothing has been published, so nothing is recorded as published — which is exactly
            // what makes the entire pre-existing history count as waiting to join the world.
            assertNull(db.syncDao().syncState(device))
            assertEquals(25_000L, db.countDao().lifetimeTotal(device))
        }

    @Test
    fun `an empty v2 database upgrades`() = runBlocking {
        V2DatabaseBuilder.create(context, dbName)
        val db = openUpgraded()
        assertEquals(0L, db.countDao().lifetimeTotal(device))
        assertNotNull(db.profileDao().deviceProfile())
    }
}
