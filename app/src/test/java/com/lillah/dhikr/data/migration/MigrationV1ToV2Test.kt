package com.lillah.dhikr.data.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.Migrations
import kotlinx.coroutines.flow.first
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
 * The upgrade path, tested against a real version 1 database.
 *
 * These are the tests standing between an existing user and losing years of counting, so they
 * assert on actual rows read back through the production DAOs rather than on the migration SQL
 * having run without throwing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationV1ToV2Test {

    private val dbName = "migration-test.db"
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

    /** Opens the v2 database over whatever is on disk, exactly as the app does. */
    private fun openUpgraded(): DhikrDatabase =
        Room.databaseBuilder(context, DhikrDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    @Test
    fun `a populated v1 database upgrades with every count intact`() = runBlocking {
        // 25,000 dhikr spread over 100 days across three adhkar — the scenario in the brief.
        V1DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertCollection(db, 1, "Morning Adhkar")
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", 1, currentCount = 17, isFavorite = true)
            V1DatabaseBuilder.insertDhikr(db, 2, "Alhamdulillah", 1, targetCount = 100)
            V1DatabaseBuilder.insertDhikr(db, 3, "My own dhikr", null, targetCount = 7)

            var remaining = 25_000
            var day = 19_000L
            while (remaining > 0) {
                val each = minOf(250, remaining)
                V1DatabaseBuilder.insertCount(db, (day % 3) + 1, day, each)
                remaining -= each
                day++
            }
            V1DatabaseBuilder.insertAchievement(db, "first_remembrance")
            V1DatabaseBuilder.insertAchievement(db, "thousand")
            V1DatabaseBuilder.insertCounter(db, "best_day", 900)
            V1DatabaseBuilder.insertCounter(db, "morning_completions", 12)
        }

        val db = openUpgraded()

        assertEquals(
            "the headline total must survive the upgrade exactly",
            25_000L,
            db.countDao().lifetimeTotal(),
        )
        assertEquals(3, db.dhikrDao().count())
        assertEquals(2, db.achievementDao().getAll().size)
        assertEquals(900L, db.counterDao().get("best_day"))
        assertEquals(12L, db.counterDao().get("morning_completions"))

        val subhanAllah = db.dhikrDao().getById(1)
        assertNotNull(subhanAllah)
        assertEquals("SubhanAllah", subhanAllah!!.name)
        assertEquals("a half-finished round must not be reset by an upgrade", 17, subhanAllah.currentCount)
        assertTrue(subhanAllah.isFavorite)
        assertEquals(1L, subhanAllah.collectionId)
        assertEquals("سُبْحَانَ اللَّهِ", subhanAllah.arabic)

        assertEquals(100, db.countDao().activeDays().size)
    }

    @Test
    fun `the new sync tables arrive empty and usable`() = runBlocking {
        V1DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
            V1DatabaseBuilder.insertCount(db, 1, 19_000, 500)
        }

        val db = openUpgraded()

        // Nothing is queued by the migration itself: an upgrade must not invent contributions.
        assertEquals(emptyList<Any>(), db.syncDao().pending())
        assertNull(db.syncDao().snapshot())
        assertEquals(500L, db.countDao().lifetimeTotal())
    }

    @Test
    fun `an empty v1 database upgrades`() = runBlocking {
        V1DatabaseBuilder.create(context, dbName)
        val db = openUpgraded()
        assertEquals(0L, db.countDao().lifetimeTotal())
        assertEquals(0, db.dhikrDao().count())
    }

    @Test
    fun `partially filled records keep their nulls rather than gaining defaults`() = runBlocking {
        V1DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "Bare dhikr", null, arabic = null)
        }

        val db = openUpgraded()
        val dhikr = db.dhikrDao().getById(1)

        assertNotNull(dhikr)
        assertNull(dhikr!!.arabic)
        assertNull(dhikr.dailyTarget)
        assertNull(dhikr.collectionId)
        assertEquals("Bare dhikr", dhikr.name)
    }

    @Test
    fun `a large database upgrades with its total unchanged`() = runBlocking {
        val days = 4_000
        V1DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
            db.beginTransaction()
            try {
                repeat(days) { index ->
                    V1DatabaseBuilder.insertCount(db, 1, 10_000L + index, 137)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        val db = openUpgraded()
        assertEquals(days * 137L, db.countDao().lifetimeTotal())
        assertEquals(days, db.countDao().activeDays().size)
    }

    @Test
    fun `reopening an already migrated database is a no-op`() = runBlocking {
        V1DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
            V1DatabaseBuilder.insertCount(db, 1, 19_000, 4_321)
        }

        openUpgraded().also { it.close() }
        database = null

        // Second open runs no migration; the data must be identical, not merely present.
        val db = openUpgraded()
        assertEquals(4_321L, db.countDao().lifetimeTotal())
        assertEquals(1, db.dhikrDao().count())
    }

    @Test
    fun `writes made after the upgrade coexist with the old history`() = runBlocking {
        V1DatabaseBuilder.create(context, dbName) { db ->
            V1DatabaseBuilder.insertDhikr(db, 1, "SubhanAllah", null)
            V1DatabaseBuilder.insertCount(db, 1, 19_000, 25_000)
        }

        val db = openUpgraded()
        db.countDao().addCount(dhikrId = 1, epochDay = 19_001, delta = 5_000, now = 1L)

        assertEquals(30_000L, db.countDao().lifetimeTotal())
        assertEquals(
            "the pre-upgrade day must be untouched by a post-upgrade write",
            25_000,
            db.countDao().observeDayTotal(19_000).first(),
        )
    }
}
