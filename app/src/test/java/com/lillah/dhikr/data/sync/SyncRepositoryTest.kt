package com.lillah.dhikr.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.data.repository.DhikrRepository
import com.lillah.dhikr.data.repository.ProfileRepository
import com.lillah.dhikr.data.repository.SyncRepository
import com.lillah.dhikr.domain.sync.AuthMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncRepositoryTest {

    private class FixedClock(var day: LocalDate = LocalDate.of(2026, 3, 4)) : AppClock {
        override fun zone(): ZoneId = ZoneId.of("UTC")
        override fun now(): Instant = day.atStartOfDay(zone()).toInstant()
        override fun today(): LocalDate = day
    }

    private lateinit var context: Context
    private lateinit var database: DhikrDatabase
    private lateinit var dhikrRepository: DhikrRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var backend: FakeBackend
    private val clock = FixedClock()
    private lateinit var profileRepository: ProfileRepository
    private val DEVICE = 1L

    @Before
    fun setUp(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DhikrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backend = FakeBackend()
        accountRepository = AccountRepository(context)
        accountRepository.clearSignedIn()

        profileRepository = ProfileRepository(
            profileDao = database.profileDao(),
            accountRepository = accountRepository,
            clock = clock,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        profileRepository.ensureDeviceProfile()

        dhikrRepository = DhikrRepository(
            database = database,
            dhikrDao = database.dhikrDao(),
            collectionDao = database.collectionDao(),
            countDao = database.countDao(),
            counterDao = database.counterDao(),
            syncDao = database.syncDao(),
            profiles = profileRepository,
            clock = clock,
        )
        syncRepository = SyncRepository(
            syncDao = database.syncDao(),
            countDao = database.countDao(),
            accountRepository = accountRepository,
            profiles = profileRepository,
            backend = backend,
            clock = clock,
        )
        database.dhikrDao().upsert(
            DhikrEntity(
                id = 1,
                name = "SubhanAllah",
                targetCount = 33,
                createdAt = 0,
                profileId = DEVICE,
            )
        )
    }

    @After
    fun tearDown(): Unit = runBlocking {
        accountRepository.clearSignedIn()
        database.close()
    }

    private suspend fun signIn(uid: String = "user-1") = accountRepository.setSignedIn(
        uid = uid, displayName = "Test", email = null, photoUrl = null, method = AuthMethod.Google,
    )

    private suspend fun count(times: Int) = repeat(times) { dhikrRepository.increment(1) }

    @Test
    fun `every count is still recorded locally`() = runBlocking {
        count(10)
        assertEquals(10L, database.countDao().lifetimeTotal(DEVICE))
        assertEquals(
            "the device keeps its own record of each count regardless of syncing",
            10,
            database.syncDao().pending(DEVICE).size,
        )
    }

    @Test
    fun `an undo lowers the local total`() = runBlocking {
        count(5)
        dhikrRepository.decrement(1)
        assertEquals(4L, database.countDao().lifetimeTotal(DEVICE))
    }

    @Test
    fun `counting offline then connecting publishes the exact total`() = runBlocking {
        signIn()
        count(500)
        assertEquals(500L, database.countDao().lifetimeTotal(DEVICE))

        assertTrue(syncRepository.syncNow().isSuccess)

        assertEquals(500L, backend.worldTotal)
        assertEquals(0L, syncRepository.status.first().pendingTotal)
    }

    @Test
    fun `a whole tasbih costs a single write`() = runBlocking {
        signIn()
        count(1_000)

        assertTrue(syncRepository.syncNow().isSuccess)

        assertEquals(
            "one thousand dhikr must not become one thousand uploads",
            1,
            backend.writeCount,
        )
        assertEquals(1_000L, backend.worldTotal)
    }

    @Test
    fun `connecting repeatedly cannot inflate the world total`() = runBlocking {
        signIn()
        count(50)

        repeat(6) { syncRepository.syncNow() }

        assertEquals(
            "an absolute total written six times is still that total",
            50L,
            backend.worldTotal,
        )
    }

    @Test
    fun `a failed connect keeps the counting and keeps it owed`() = runBlocking {
        signIn()
        count(120)
        backend.failNextPublish = java.io.IOException("network down")

        assertTrue(syncRepository.syncNow().isFailure)

        assertEquals("a failure must never touch what was counted", 120L, database.countDao().lifetimeTotal(DEVICE))
        assertEquals(0L, backend.worldTotal)
        assertEquals(120L, syncRepository.status.first().pendingTotal)

        // Retrying settles it, at the right figure rather than a doubled one.
        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(120L, backend.worldTotal)
        assertEquals(0L, syncRepository.status.first().pendingTotal)
    }

    @Test
    fun `history counted before ever connecting is owed in full`() = runBlocking {
        // A device upgrading from an older version: counts in the ledger, never synced.
        database.countDao().addCount(dhikrId = 1, epochDay = 19_000, delta = 25_000, now = 0)
        signIn()

        assertEquals(
            "nothing had to be migrated for the whole history to be waiting",
            25_000L,
            syncRepository.status.first().pendingTotal,
        )

        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(25_000L, backend.worldTotal)
    }

    @Test
    fun `counting after a connect only owes the difference`() = runBlocking {
        database.countDao().addCount(dhikrId = 1, epochDay = 19_000, delta = 25_000, now = 0)
        signIn()
        syncRepository.syncNow()

        count(500)

        assertEquals(500L, syncRepository.status.first().pendingTotal)
        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(25_500L, backend.worldTotal)
        assertEquals(25_500L, database.countDao().lifetimeTotal(DEVICE))
    }

    @Test
    fun `a lost read-back keeps the contribution but is reported`() = runBlocking {
        signIn()
        count(40)
        backend.failNextFetch = java.io.IOException("read failed")

        // The contribution is not rolled back: it landed, and nothing is owed any more.
        val result = syncRepository.syncNow()
        assertEquals(40L, backend.worldTotal)
        assertEquals(0L, syncRepository.status.first().pendingTotal)

        // But the board has no figure to show, so the failure has to reach the screen rather
        // than being swallowed — otherwise the user reads a blank world count as a bug.
        assertTrue("the read-back failure is reported", result.isFailure)
        assertNotNull(syncRepository.status.first().lastError)
    }

    @Test
    fun `a world total still shows when today's figure cannot be read`() = runBlocking {
        // Today's figure is a filtered aggregation and needs an index the lifetime total does
        // not, so the server can answer one and refuse the other. That must not blank the board.
        signIn()
        count(90)
        backend.todayFigureUnknown = true

        assertTrue(syncRepository.syncNow().isSuccess)

        val figures = syncRepository.cachedFigures.first()
        assertNotNull("the board still has a worldwide total to show", figures)
        assertEquals(90L, figures!!.globalTotal)
        assertNull("an unread figure stays unknown rather than becoming a zero", figures.globalToday)
    }

    @Test
    fun `a known today figure of zero is kept as zero`() = runBlocking {
        // The mirror of the case above: nobody has counted yet today, and that is a real answer.
        signIn()
        database.countDao().addCount(dhikrId = 1, epochDay = 18_000, delta = 90, now = 0)

        assertTrue(syncRepository.syncNow().isSuccess)

        assertEquals(0L, syncRepository.cachedFigures.first()?.globalToday)
    }

    @Test
    fun `signing out leaves everything owed and nothing lost`() = runBlocking {
        signIn()
        count(30)
        backend.failNextPublish = java.io.IOException("offline")
        syncRepository.syncNow()

        accountRepository.clearSignedIn()

        assertEquals(30L, database.countDao().lifetimeTotal(DEVICE))
        assertFalse(accountRepository.state.first().isSignedIn)

        signIn()
        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(30L, backend.worldTotal)
    }

    @Test
    fun `connecting without an account is refused without touching anything`() = runBlocking {
        count(7)
        assertTrue(syncRepository.syncNow().isFailure)
        assertEquals(7L, database.countDao().lifetimeTotal(DEVICE))
        assertEquals(0L, backend.worldTotal)
    }

    @Test
    fun `status reports what is still owed to the world count`() = runBlocking {
        signIn()
        count(42)

        val status = syncRepository.status.first()
        assertTrue(status.signedIn)
        assertTrue(status.hasPending)
        assertEquals(42L, status.pendingTotal)
    }

    @Test
    fun `two people on one phone publish separately`() = runBlocking {
        // The device profile counts, then the first account signs in and adopts it.
        count(300)
        val first = profileRepository.onSignedIn(
            com.lillah.dhikr.domain.sync.AuthUser("uid-a", "Aisha", null, null, AuthMethod.Google)
        )
        assertEquals("the first account adopts the device profile", 1L, first.profileId)
        accountRepository.setSignedIn("uid-a", "Aisha", null, null, AuthMethod.Google)
        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(300L, backend.contributions["uid-a"])

        // A second profile on the same phone, signed in as somebody else.
        val second = profileRepository.onSignedIn(
            com.lillah.dhikr.domain.sync.AuthUser("uid-b", "Bilal", null, null, AuthMethod.Google)
        )
        assertNotEquals("a second person gets their own profile", 1L, second.profileId)
        accountRepository.setSignedIn("uid-b", "Bilal", null, null, AuthMethod.Google)
        database.dhikrDao().upsert(
            com.lillah.dhikr.data.local.entity.DhikrEntity(
                id = 2, name = "Alhamdulillah", createdAt = 0, profileId = second.profileId,
            )
        )
        repeat(70) { dhikrRepository.increment(2) }
        assertTrue(syncRepository.syncNow().isSuccess)

        assertEquals(70L, backend.contributions["uid-b"])
        assertEquals("neither total absorbed the other", 300L, backend.contributions["uid-a"])
        assertEquals(370L, backend.worldTotal)
    }
}
