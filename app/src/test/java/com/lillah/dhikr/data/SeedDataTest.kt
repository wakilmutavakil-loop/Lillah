package com.lillah.dhikr.data

import com.lillah.dhikr.data.seed.SeedData
import com.lillah.dhikr.domain.model.CollectionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {

    private val morning = SeedData.morning(1)
    private val evening = SeedData.evening(2)
    private val essentials = SeedData.essentials(3)
    private val afterPrayer = SeedData.afterPrayer(4)
    private val all = morning + evening + essentials + afterPrayer

    @Test
    fun `the four shipped collections are present and distinct`() {
        val kinds = SeedData.collections.map { CollectionKind.fromName(it.kind) }
        assertEquals(
            listOf(
                CollectionKind.Morning,
                CollectionKind.Evening,
                CollectionKind.Essentials,
                CollectionKind.AfterPrayer,
            ),
            kinds,
        )
    }

    @Test
    fun `seeded adhkar land in the collection they were asked for`() {
        assertTrue(morning.all { it.collectionId == 1L })
        assertTrue(evening.all { it.collectionId == 2L })
        assertTrue(essentials.all { it.collectionId == 3L })
        assertTrue(afterPrayer.all { it.collectionId == 4L })
    }

    /**
     * Restore matches on name within a collection, so a duplicate name would make one of the pair
     * permanently unrestorable.
     */
    @Test
    fun `names are unique within each collection`() {
        listOf(morning, evening, essentials, afterPrayer).forEach { collection ->
            val names = collection.map { it.name }
            assertEquals(
                "duplicate name in ${names.firstOrNull()}'s collection",
                names.size,
                names.toSet().size,
            )
        }
    }

    @Test
    fun `every seeded dhikr is usable`() {
        all.forEach { dhikr ->
            assertTrue("blank name", dhikr.name.isNotBlank())
            assertTrue("target must be positive: ${dhikr.name}", dhikr.targetCount > 0)
            assertTrue("built-in flag missing: ${dhikr.name}", dhikr.isBuiltIn)
        }
    }

    @Test
    fun `every seeded dhikr carries Arabic and a meaning`() {
        all.forEach { dhikr ->
            assertTrue("no Arabic for ${dhikr.name}", !dhikr.arabic.isNullOrBlank())
            assertTrue("no meaning for ${dhikr.name}", !dhikr.meaning.isNullOrBlank())
            assertTrue(
                "no transliteration for ${dhikr.name}",
                !dhikr.transliteration.isNullOrBlank(),
            )
        }
    }

    @Test
    fun `Arabic text is actually Arabic script`() {
        val arabicRange = '؀'..'ۿ'
        all.forEach { dhikr ->
            val arabic = dhikr.arabic.orEmpty()
            assertTrue(
                "text for ${dhikr.name} is not Arabic script",
                arabic.any { it in arabicRange },
            )
        }
    }

    @Test
    fun `sort order is stable within a collection`() {
        listOf(morning, evening, essentials, afterPrayer).forEach { collection ->
            val orders = collection.map { it.sortOrder }
            assertEquals(orders.size, orders.toSet().size)
            assertEquals(orders.sorted(), orders)
        }
    }

    @Test
    fun `the after-prayer tasbih keeps its traditional counts`() {
        val counts = afterPrayer
            .filter { it.name in setOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar") }
            .map { it.targetCount }
        assertEquals(listOf(33, 33, 33), counts)
    }
}
