package com.lillah.dhikr.data.counts

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.repository.DhikrRepository
import com.lillah.dhikr.data.repository.ProfileRepository
import com.lillah.dhikr.data.prefs.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Editing a dhikr must never touch what has been counted with it.
 *
 * `dhikr_counts` cascades on the delete of its dhikr, which is right — a dhikr that no longer
 * exists should not leave orphan rows. What made that dangerous is that saving an edit went
 * through `INSERT OR REPLACE`, and SQLite implements REPLACE as a *delete followed by an insert*.
 * The delete fired the cascade, so changing "repetitions per round" silently erased every day
 * that dhikr had ever been counted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditingKeepsHistoryTest {

    private class FixedClock(var day: LocalDate = LocalDate.of(2026, 3, 4)) : AppClock {
        override fun zone(): ZoneId = ZoneId.of("UTC")
        override fun now(): Instant = day.atStartOfDay(zone()).toInstant()
        override fun today(): LocalDate = day
    }

    private lateinit var context: Context
    private lateinit var database: DhikrDatabase
    private lateinit var dhikr: DhikrRepository
    private lateinit var accounts: AccountRepository
    private val clock = FixedClock()
    private val DEVICE = 1L

    @Before
    fun setUp(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DhikrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accounts = AccountRepository(context)
        accounts.clearSignedIn()
        val profiles = ProfileRepository(
            profileDao = database.profileDao(),
            accountRepository = accounts,
            clock = clock,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        profiles.ensureDeviceProfile()
        dhikr = DhikrRepository(
            database = database,
            dhikrDao = database.dhikrDao(),
            collectionDao = database.collectionDao(),
            countDao = database.countDao(),
            counterDao = database.counterDao(),
            syncDao = database.syncDao(),
            profiles = profiles,
            clock = clock,
        )
        dhikr.seedIfEmpty(DEVICE)
    }

    @After
    fun tearDown(): Unit = runBlocking {
        accounts.clearSignedIn()
        database.close()
    }

    /** Counts [times] on [id], on [on]. */
    private suspend fun count(id: Long, times: Int, on: LocalDate) {
        clock.day = on
        repeat(times) { dhikr.increment(id) }
    }

    @Test
    fun `changing repetitions per round keeps every day already counted`() = runBlocking {
        val id = dhikr.observeAll().first().first().id

        count(id, 40, LocalDate.of(2026, 3, 1))
        count(id, 60, LocalDate.of(2026, 3, 2))
        count(id, 33, LocalDate.of(2026, 3, 4))
        assertEquals(133L, database.countDao().lifetimeTotal(DEVICE))

        // Exactly what the counter's "Repetitions per round" sheet does on Save.
        val before = dhikr.getDhikr(id)!!
        dhikr.upsert(before.copy(targetCount = 100))

        assertEquals(
            "changing the target must not erase a single day of counting",
            133L,
            database.countDao().lifetimeTotal(DEVICE),
        )
        assertEquals(40, database.countDao().dayTotal(DEVICE, LocalDate.of(2026, 3, 1).toEpochDay()))
        assertEquals(60, database.countDao().dayTotal(DEVICE, LocalDate.of(2026, 3, 2).toEpochDay()))
        assertEquals(100, dhikr.getDhikr(id)!!.targetCount)
    }

    @Test
    fun `editing a dhikr's words keeps its history`() = runBlocking {
        val id = dhikr.observeAll().first().first().id
        count(id, 99, LocalDate.of(2026, 3, 3))

        val before = dhikr.getDhikr(id)!!
        dhikr.upsert(before.copy(name = "Renamed", meaning = "A new meaning", accentIndex = 4))

        assertEquals(99L, database.countDao().lifetimeTotal(DEVICE))
        assertEquals("Renamed", dhikr.getDhikr(id)!!.name)
    }

    @Test
    fun `editing a collection keeps the adhkar inside it`() = runBlocking {
        val collection = dhikr.observeCollections().first().first()
        val insideBefore = dhikr.observeAll().first().count { it.collectionId == collection.id }
        assertEquals("the fixture needs adhkar in the collection", true, insideBefore > 0)

        dhikr.upsertCollection(collection.copy(name = "Renamed collection"))

        assertEquals(
            "renaming a collection must not empty it",
            insideBefore,
            dhikr.observeAll().first().count { it.collectionId == collection.id },
        )
    }
}
