package com.lillah.dhikr.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Dhikr(
    val id: Long = 0,
    val name: String = "",
    val arabic: String? = null,
    val transliteration: String? = null,
    val meaning: String? = null,
    val virtue: String? = null,
    val source: String? = null,
    val targetCount: Int = 33,
    val dailyTarget: Int? = null,
    val collectionId: Long? = null,
    val sortOrder: Int = 0,
    val accentIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isBuiltIn: Boolean = false,
    val currentCount: Int = 0,
    val roundsToday: Int = 0,
    val roundsEpochDay: Long = 0,
    val lastCountedAt: Long? = null,
    val createdAt: Long = 0,
    val profileId: Long = 1,
) {
    val safeTarget: Int get() = targetCount.coerceAtLeast(1)

    /** 0f..1f through the current round. A finished round reads as a full ring, not an empty one. */
    val roundProgress: Float
        get() = (currentCount.toFloat() / safeTarget).coerceIn(0f, 1f)

    val isRoundComplete: Boolean get() = currentCount >= safeTarget

    val displayTitle: String get() = name.ifBlank { transliteration.orEmpty().ifBlank { "Dhikr" } }
}
