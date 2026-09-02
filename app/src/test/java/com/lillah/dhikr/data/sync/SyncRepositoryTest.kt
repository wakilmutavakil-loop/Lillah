package com.lillah.dhikr.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.data.repository.DhikrRepository
import com.lillah.dhikr.data.repository.SyncRepository
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.SyncOperationKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Before
    fun setUp(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DhikrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backend = FakeBackend()
        accountRepository = AccountRepository(context)
        accountRepository.clearSignedIn()

        dhikrRepository = DhikrRepository(
            database = database,
            dhikrDao = database.dhikrDao(),
            collectionDao = database.collectionDao(),
            countDao = database.countDao(),
            counterDao = database.counterDao(),
            syncDao = database.syncDao(),
            clock = clock,
        )
        syncRepository = SyncRepository(
            syncDao = database.syncDao(),
            countDao = database.countDao(),
            accountRepository = accountRepository,
            backend = backend,
            clock = clock,
        )
        database.dhikrDao().insert(
            DhikrEntity(id = 1, name = "SubhanAllah", targetCount = 33, createdAt = 0)
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
    fun `each count queues exactly one operation`() = runBlocking {
        count(10)
        val pending = database.syncDao().pending()
        assertEquals(10, pending.size)
        assertEquals("operation ids must be unique", 10, pending.map { it.opId }.toSet().size)
        assertTrue(pending.all { it.kind == SyncOperationKind.COUNT_DELTA.name })
        assertEquals(10L, database.syncDao().observePendingTotal().first())
    }

    @Test
    fun `an undo queues a compensating operation rather than removing one`() = runBlocking {
        count(5)
        dhikrRepository.decrement(1)

        val pending = database.syncDao().pending()
        assertEquals("nothing is ever removed from the outbox", 6, pending.size)
        assertEquals(4L, database.syncDao().observePendingTotal().first())
        assertEquals(4L, database.countDao().lifetimeTotal())
    }

    @Test
    fun `counting offline then syncing uploads the exact total`() = runBlocking {
        signIn()
        count(500)
        assertEquals(500L, database.countDao().lifetimeTotal())

        assertTrue(syncRepository.syncNow().isSuccess)

        assertEquals(500L, backend.serverUserTotal)
        assertEquals(0, database.syncDao().pending().size)
    }

    @Test
    fun `a failed push keeps every operation queued`() = runBlocking {
        signIn()
        count(120)
        backend.failNextPush = java.io.IOException("network down")

        assertTrue(syncRepository.syncNow().isFailure)

        assertEquals("a failure must never discard local work", 120, database.syncDao().pending().size)
        assertEquals(120L, database.countDao().lifetimeTotal())
        assertEquals(0L, backend.serverUserTotal)

        // The retry succeeds and the total is right — not doubled by the earlier attempt.
        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(120L, backend.serverUserTotal)
        assertEquals(0, database.syncDao().pending().size)
    }

    @Test
    fun `retrying a push cannot double count`() = runBlocking {
        signIn()
        count(50)

        // Two full syncs, and a third after re-queueing everything as if confirmation was lost.
        syncRepository.syncNow()
        syncRepository.syncNow()
        val ids = backend.accepted.keys.toList()
        database.syncDao().markFailed(ids, "confirmation lost", 0)
        syncRepository.syncNow()

        assertEquals("the server keys by operation id, so duplicates collapse", 50L, backend.serverUserTotal)
        assertTrue("the same ids really were pushed more than once", backend.pushAttempts.size > 1)
    }

    @Test
    fun `existing history from before the outbox is claimed once`() = runBlocking {
        // A device upgrading from v1.0.0: counts in the ledger, nothing in the outbox.
        database.countDao().addCount(dhikrId = 1, epochDay = 19_000, delta = 25_000, now = 0)
        assertEquals(25_000L, database.countDao().lifetimeTotal())
        assertEquals(0, database.syncDao().pending().size)

        signIn()
        syncRepository.claimExistingHistory()

        val baseline = database.syncDao().pending().single()
        assertEquals(SyncOperationKind.BASELINE.name, baseline.kind)
        assertEquals(25_000L, baseline.delta)

        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(25_000L, backend.serverUserTotal)
    }

    @Test
    fun `claiming history repeatedly stays at one operation`() = runBlocking {
        database.countDao().addCount(dhikrId = 1, epochDay = 19_000, delta = 12_500, now = 0)
        signIn()

        repeat(5) { syncRepository.claimExistingHistory() }
        syncRepository.syncNow()
        repeat(5) { syncRepository.claimExistingHistory() }
        syncRepository.syncNow()

        assertEquals(
            "12,500 before the upgrade must still be 12,500 after it",
            12_500L,
            backend.serverUserTotal,
        )
    }

    @Test
    fun `the baseline excludes counts already queued after the upgrade`() = runBlocking {
        database.countDao().addCount(dhikrId = 1, epochDay = 19_000, delta = 25_000, now = 0)
        count(500) // counted after upgrading, before signing in — already in the outbox

        signIn()
        syncRepository.claimExistingHistory()
        assertTrue(syncRepository.syncNow().isSuccess)

        assertEquals(
            "history plus post-upgrade counting, each counted once",
            25_500L,
            backend.serverUserTotal,
        )
        assertEquals(25_500L, database.countDao().lifetimeTotal())
    }

    @Test
    fun `signing out leaves the queue and the history alone`() = runBlocking {
        signIn()
        count(30)
        backend.failNextPush = java.io.IOException("offline")
        syncRepository.syncNow()

        accountRepository.clearSignedIn()

        assertEquals(30, database.syncDao().pending().size)
        assertEquals(30L, database.countDao().lifetimeTotal())
        assertFalse(accountRepository.state.first().isSignedIn)

        // Signing back in finds the work still waiting.
        signIn()
        assertTrue(syncRepository.syncNow().isSuccess)
        assertEquals(30L, backend.serverUserTotal)
    }

    @Test
    fun `syncing without an account is refused without touching the queue`() = runBlocking {
        count(7)
        assertTrue(syncRepository.syncNow().isFailure)
        assertEquals(7, database.syncDao().pending().size)
        assertEquals(0L, backend.serverUserTotal)
    }

    @Test
    fun `status reports what is still waiting`() = runBlocking {
        signIn()
        count(42)

        val status = syncRepository.status.first()
        assertTrue(status.signedIn)
        assertTrue(status.hasPending)
        assertEquals(42, status.pendingOperations)
        assertEquals(42L, status.pendingTotal)
    }

    @Test
    fun `a synced batch is marked so it is not uploaded again`() = runBlocking {
        signIn()
        count(20)
        syncRepository.syncNow()

        assertEquals(0, database.syncDao().pending().size)
        assertEquals(0L, database.syncDao().observePendingTotal().first())
        // Synced rows stay in the table, marked, rather than being deleted: the record of what
        // was contributed is what makes the baseline calculation correct later.
        assertEquals(20L, database.syncDao().countDeltaTotal())
        assertEquals(20, backend.accepted.size)
    }
}
