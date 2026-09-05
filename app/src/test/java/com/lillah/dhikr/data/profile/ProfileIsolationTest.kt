package com.lillah.dhikr.data.profile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.DhikrDatabase
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.data.prefs.AccountRepository
import com.lillah.dhikr.data.repository.DhikrRepository
import com.lillah.dhikr.data.repository.ProfileRepository
import com.lillah.dhikr.data.repository.StatsRepository
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.AuthUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

/**
 * Several people, one phone, all of it local.
 *
 * The rules being checked: the first person to sign in inherits whatever the device already had,
 * a second person starts clean and can never see the first person's counting, and signing out
 * hides data rather than removing it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileIsolationTest {

    private class FixedClock : AppClock {
        override fun zone(): ZoneId = ZoneId.of("UTC")
        override fun now(): Instant = LocalDate.of(2026, 3, 4).atStartOfDay(zone()).toInstant()
        override fun today(): LocalDate = LocalDate.of(2026, 3, 4)
    }

    private lateinit var context: Context
    private lateinit var database: DhikrDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var profiles: ProfileRepository
    private lateinit var dhikr: DhikrRepository
    private lateinit var stats: StatsRepository
    private val clock = FixedClock()

    private fun user(uid: String, name: String) =
        AuthUser(uid, name, "$name@example.com", null, AuthMethod.Google)

    @Before
    fun setUp(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DhikrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accounts = AccountRepository(context)
        accounts.clearSignedIn()
        accounts.setActiveProfile(1)

        profiles = ProfileRepository(
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
        stats = StatsRepository(database.countDao(), profiles, clock)
    }

    @After
    fun tearDown(): Unit = runBlocking {
        accounts.clearSignedIn()
        accounts.setActiveProfile(1)
        database.close()
    }

    private suspend fun addDhikr(name: String): Long =
        dhikr.upsert(com.lillah.dhikr.domain.model.Dhikr(name = name, targetCount = 33))

    private suspend fun count(id: Long, times: Int) = repeat(times) { dhikr.increment(id) }

    @Test
    fun `the first account to sign in inherits what the device already counted`() = runBlocking {
        val id = addDhikr("SubhanAllah")
        count(id, 25_000)
        assertEquals(25_000L, stats.observeLifetimeTotal().first())

        val resolution = profiles.onSignedIn(user("uid-a", "Aisha"))

        assertEquals("the device profile is adopted, not replaced", 1L, resolution.profileId)
        assertFalse("so nothing needs seeding", resolution.isNew)
        assertEquals(
            "25,000 before signing in is 25,000 after",
            25_000L,
            stats.observeLifetimeTotal().first(),
        )
        assertEquals("uid-a", database.profileDao().deviceProfile()?.uid)
    }

    @Test
    fun `a second account gets its own profile and cannot see the first`() = runBlocking {
        val id = addDhikr("SubhanAllah")
        count(id, 25_000)
        profiles.onSignedIn(user("uid-a", "Aisha"))

        val second = profiles.onSignedIn(user("uid-b", "Bilal"))

        assertNotEquals("a different person is a different profile", 1L, second.profileId)
        assertTrue("and needs its own adhkar", second.isNew)
        assertEquals(
            "the second person starts at zero, not at somebody else's total",
            0L,
            stats.observeLifetimeTotal().first(),
        )
        assertTrue(dhikr.observeAll().first().isEmpty())
    }

    @Test
    fun `each profile counts into its own history`() = runBlocking {
        val deviceDhikr = addDhikr("SubhanAllah")
        count(deviceDhikr, 900)
        profiles.onSignedIn(user("uid-a", "Aisha"))

        profiles.onSignedIn(user("uid-b", "Bilal"))
        val bilalDhikr = addDhikr("Alhamdulillah")
        count(bilalDhikr, 40)
        assertEquals(40L, stats.observeLifetimeTotal().first())

        // Back to the first account: their history is exactly where they left it.
        profiles.onSignedIn(user("uid-a", "Aisha"))
        assertEquals(900L, stats.observeLifetimeTotal().first())
        assertEquals(1, dhikr.observeAll().first().size)
        assertEquals("SubhanAllah", dhikr.observeAll().first().single().name)

        // And so is the second account's.
        profiles.onSignedIn(user("uid-b", "Bilal"))
        assertEquals(40L, stats.observeLifetimeTotal().first())
        assertEquals("Alhamdulillah", dhikr.observeAll().first().single().name)
    }

    @Test
    fun `signing out returns to the device profile without removing anything`() = runBlocking {
        val id = addDhikr("SubhanAllah")
        count(id, 500)
        profiles.onSignedIn(user("uid-a", "Aisha"))
        profiles.onSignedIn(user("uid-b", "Bilal"))
        val bilal = addDhikr("Alhamdulillah")
        count(bilal, 70)

        accounts.clearSignedIn()

        assertEquals(1L, accounts.state.first().activeProfileId)
        assertEquals("the device profile still holds its 500", 500L, stats.observeLifetimeTotal().first())

        // Bilal's counting was not touched; signing back in finds it intact.
        profiles.onSignedIn(user("uid-b", "Bilal"))
        assertEquals(70L, stats.observeLifetimeTotal().first())
        assertEquals(2, database.profileDao().count())
    }

    @Test
    fun `returning to an account never creates a duplicate profile`() = runBlocking {
        profiles.onSignedIn(user("uid-a", "Aisha"))
        repeat(4) { profiles.onSignedIn(user("uid-a", "Aisha")) }
        profiles.onSignedIn(user("uid-b", "Bilal"))
        repeat(3) { profiles.onSignedIn(user("uid-b", "Bilal")) }

        assertEquals(2, database.profileDao().count())
    }

    @Test
    fun `achievements and counters are per person`() = runBlocking {
        database.counterDao().increment(1, "goal_days", 12)
        profiles.onSignedIn(user("uid-a", "Aisha"))
        assertEquals(12L, database.counterDao().get(1, "goal_days"))

        val second = profiles.onSignedIn(user("uid-b", "Bilal"))
        assertNull(
            "a new person does not inherit somebody else's milestones",
            database.counterDao().get(second.profileId, "goal_days"),
        )
    }

    @Test
    fun `a new profile is seeded with its own copy of the shipped adhkar`() = runBlocking {
        profiles.onSignedIn(user("uid-a", "Aisha"))
        dhikr.seedIfEmpty(1)
        val firstCount = dhikr.observeAll().first().size
        assertTrue(firstCount > 0)

        val second = profiles.onSignedIn(user("uid-b", "Bilal"))
        dhikr.seedIfEmpty(second.profileId)

        assertEquals(
            "the same starting set, not a share of the first person's",
            firstCount,
            dhikr.observeAll().first().size,
        )
        assertTrue(dhikr.observeAll().first().all { it.profileId == second.profileId })
    }

    @Test
    fun `an entity insert defaults to the device profile`() = runBlocking {
        // Guards the migration's DEFAULT 1: a row written without an explicit profile must not
        // become invisible to everyone.
        database.dhikrDao().upsert(DhikrEntity(name = "Legacy row", createdAt = 0))
        assertEquals(1, database.dhikrDao().count(1))
    }
}
