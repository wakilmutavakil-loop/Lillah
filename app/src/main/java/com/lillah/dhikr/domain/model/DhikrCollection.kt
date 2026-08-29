package com.lillah.dhikr.domain.model

import androidx.compose.runtime.Immutable

enum class CollectionKind {
    Morning, Evening, Essentials, AfterPrayer, Custom;

    companion object {
        fun fromName(value: String?): CollectionKind =
            entries.firstOrNull { it.name == value } ?: Custom
    }
}

/** Identifies which built-in vector artwork backs a collection cover. */
enum class CoverArt {
    Sunrise, Night, Beads, Arch, Bloom, Waves;

    companion object {
        fun fromKey(value: String?): CoverArt =
            entries.firstOrNull { it.name == value } ?: Bloom
    }
}

@Immutable
data class DhikrCollection(
    val id: Long = 0,
    val name: String = "",
    val arabicName: String? = null,
    val description: String? = null,
    val kind: CollectionKind = CollectionKind.Custom,
    val artwork: CoverArt = CoverArt.Bloom,
    val coverImagePath: String? = null,
    val accentIndex: Int = 0,
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = false,
)

/** A collection with the day's progress folded in, ready for the Collections grid. */
@Immutable
data class CollectionProgress(
    val collection: DhikrCollection,
    val itemCount: Int,
    val completedToday: Int,
    val totalToday: Int,
) {
    val fraction: Float
        get() = if (itemCount == 0) 0f else completedToday.toFloat() / itemCount
    val isComplete: Boolean get() = itemCount > 0 && completedToday >= itemCount
}

/** A dhikr paired with how far through it the user is today. */
@Immutable
data class DhikrProgress(
    val dhikr: Dhikr,
    val countToday: Int,
) {
    val goal: Int get() = dhikr.dailyTarget ?: dhikr.safeTarget
    val fraction: Float get() = (countToday.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isCompleteToday: Boolean get() = countToday >= goal
}
