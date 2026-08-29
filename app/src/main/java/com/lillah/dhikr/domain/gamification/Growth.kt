package com.lillah.dhikr.domain.gamification

import androidx.compose.runtime.Immutable

/**
 * Lifetime progress is framed as a garden rather than a score or a rank. It grows, it never
 * shrinks, and there is nobody else's garden to compare it against.
 */
enum class GrowthStage(
    val displayName: String,
    val threshold: Long,
    val blurb: String,
) {
    Seed("Seed", 0, "Something has been planted. Keep it watered."),
    Sprout("Sprout", 100, "First green. The habit is finding its shape."),
    Sapling("Sapling", 500, "Standing on its own now."),
    Blossom("Blossom", 2_000, "It has started to give something back."),
    Grove("Grove", 10_000, "No longer one plant, but many."),
    Orchard("Orchard", 50_000, "Deep roots. Seasons of steady remembrance."),
    Garden("Garden", 150_000, "A quiet, well-tended place. Built one dhikr at a time.");

    companion object {
        fun forTotal(total: Long): GrowthStage =
            entries.lastOrNull { total >= it.threshold } ?: Seed

        fun next(stage: GrowthStage): GrowthStage? =
            entries.getOrNull(stage.ordinal + 1)
    }
}

@Immutable
data class GrowthState(
    val total: Long = 0,
    val stage: GrowthStage = GrowthStage.Seed,
    val nextStage: GrowthStage? = GrowthStage.Sprout,
) {
    /** 0f..1f between this stage's threshold and the next one. Full when fully grown. */
    val progressToNext: Float
        get() {
            val next = nextStage ?: return 1f
            val span = (next.threshold - stage.threshold).coerceAtLeast(1)
            return ((total - stage.threshold).toFloat() / span).coerceIn(0f, 1f)
        }

    val remainingToNext: Long
        get() = nextStage?.let { (it.threshold - total).coerceAtLeast(0) } ?: 0

    companion object {
        fun forTotal(total: Long): GrowthState {
            val stage = GrowthStage.forTotal(total)
            return GrowthState(total, stage, GrowthStage.next(stage))
        }
    }
}
