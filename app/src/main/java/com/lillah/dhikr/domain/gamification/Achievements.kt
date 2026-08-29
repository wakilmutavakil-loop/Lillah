package com.lillah.dhikr.domain.gamification

import androidx.compose.runtime.Immutable

/** Everything an achievement can be measured against. Keeps unlock rules declarative. */
enum class Metric {
    LifetimeTotal,
    CurrentStreak,
    ActiveDays,
    MorningCompletions,
    EveningCompletions,
    GoalDays,
    CustomDhikrCreated,
    CollectionsCreated,
    BestSession,
    BestDay,
}

enum class AchievementIcon {
    Sparkle, Seedling, Sunrise, Moon, Flame, Ring, Heart, Star, Mountain, Feather, Compass, Bloom,
}

@Immutable
data class AchievementDef(
    val key: String,
    val title: String,
    val description: String,
    /** Shown once unlocked. Warm, never boastful. */
    val note: String,
    val metric: Metric,
    val goal: Long,
    val icon: AchievementIcon,
    val accentIndex: Int,
)

@Immutable
data class GamificationSnapshot(
    val lifetimeTotal: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val activeDays: Int = 0,
    val morningCompletions: Long = 0,
    val eveningCompletions: Long = 0,
    val goalDays: Long = 0,
    val customDhikrCreated: Long = 0,
    val collectionsCreated: Long = 0,
    val bestSession: Long = 0,
    val bestDay: Long = 0,
) {
    fun valueOf(metric: Metric): Long = when (metric) {
        Metric.LifetimeTotal -> lifetimeTotal
        Metric.CurrentStreak -> currentStreak.toLong()
        Metric.ActiveDays -> activeDays.toLong()
        Metric.MorningCompletions -> morningCompletions
        Metric.EveningCompletions -> eveningCompletions
        Metric.GoalDays -> goalDays
        Metric.CustomDhikrCreated -> customDhikrCreated
        Metric.CollectionsCreated -> collectionsCreated
        Metric.BestSession -> bestSession
        Metric.BestDay -> bestDay
    }
}

@Immutable
data class AchievementStatus(
    val def: AchievementDef,
    val progress: Long,
    val unlockedAt: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val fraction: Float
        get() = (progress.toFloat() / def.goal.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/**
 * Milestones, not trophies. The wording stays in the register of encouragement — these mark that
 * something quiet has been kept up, and none of them can be lost once earned.
 */
object AchievementCatalog {

    val all: List<AchievementDef> = listOf(
        AchievementDef(
            key = "first_remembrance",
            title = "First Remembrance",
            description = "Count your very first dhikr.",
            note = "Everything begins somewhere. This is your somewhere.",
            metric = Metric.LifetimeTotal, goal = 1,
            icon = AchievementIcon.Sparkle, accentIndex = 0,
        ),
        AchievementDef(
            key = "hundred",
            title = "One Hundred",
            description = "Reach 100 remembrances in total.",
            note = "A hundred quiet moments already behind you.",
            metric = Metric.LifetimeTotal, goal = 100,
            icon = AchievementIcon.Seedling, accentIndex = 4,
        ),
        AchievementDef(
            key = "thousand",
            title = "One Thousand",
            description = "Reach 1,000 remembrances in total.",
            note = "Built entirely out of small, ordinary days.",
            metric = Metric.LifetimeTotal, goal = 1_000,
            icon = AchievementIcon.Star, accentIndex = 1,
        ),
        AchievementDef(
            key = "ten_thousand",
            title = "Ten Thousand",
            description = "Reach 10,000 remembrances in total.",
            note = "Steadiness carried you here, not intensity.",
            metric = Metric.LifetimeTotal, goal = 10_000,
            icon = AchievementIcon.Mountain, accentIndex = 5,
        ),
        AchievementDef(
            key = "hundred_thousand",
            title = "A Hundred Thousand",
            description = "Reach 100,000 remembrances in total.",
            note = "A long road, walked one step at a time.",
            metric = Metric.LifetimeTotal, goal = 100_000,
            icon = AchievementIcon.Bloom, accentIndex = 2,
        ),
        AchievementDef(
            key = "streak_3",
            title = "Three Days",
            description = "Remember on three days in a row.",
            note = "The hardest part of any habit is its beginning.",
            metric = Metric.CurrentStreak, goal = 3,
            icon = AchievementIcon.Flame, accentIndex = 3,
        ),
        AchievementDef(
            key = "streak_7",
            title = "A Full Week",
            description = "Remember on seven days in a row.",
            note = "Seven days is where a habit starts to hold on its own.",
            metric = Metric.CurrentStreak, goal = 7,
            icon = AchievementIcon.Flame, accentIndex = 3,
        ),
        AchievementDef(
            key = "streak_30",
            title = "Thirty Days",
            description = "Remember on thirty days in a row.",
            note = "A month of showing up. That is the whole thing.",
            metric = Metric.CurrentStreak, goal = 30,
            icon = AchievementIcon.Flame, accentIndex = 2,
        ),
        AchievementDef(
            key = "streak_100",
            title = "One Hundred Days",
            description = "Remember on a hundred days in a row.",
            note = "Rare, and quietly remarkable.",
            metric = Metric.CurrentStreak, goal = 100,
            icon = AchievementIcon.Mountain, accentIndex = 1,
        ),
        AchievementDef(
            key = "morning_first",
            title = "Morning Light",
            description = "Complete the Morning Adhkar once.",
            note = "A good way to hand the day over.",
            metric = Metric.MorningCompletions, goal = 1,
            icon = AchievementIcon.Sunrise, accentIndex = 3,
        ),
        AchievementDef(
            key = "morning_seven",
            title = "Seven Mornings",
            description = "Complete the Morning Adhkar seven times.",
            note = "Your mornings have a shape now.",
            metric = Metric.MorningCompletions, goal = 7,
            icon = AchievementIcon.Sunrise, accentIndex = 3,
        ),
        AchievementDef(
            key = "evening_first",
            title = "Evening Calm",
            description = "Complete the Evening Adhkar once.",
            note = "A gentle place to set the day down.",
            metric = Metric.EveningCompletions, goal = 1,
            icon = AchievementIcon.Moon, accentIndex = 1,
        ),
        AchievementDef(
            key = "evening_seven",
            title = "Seven Evenings",
            description = "Complete the Evening Adhkar seven times.",
            note = "Seven nights closed the same way.",
            metric = Metric.EveningCompletions, goal = 7,
            icon = AchievementIcon.Moon, accentIndex = 1,
        ),
        AchievementDef(
            key = "goal_first",
            title = "Goal Reached",
            description = "Meet your daily goal for the first time.",
            note = "You set the number yourself. That matters.",
            metric = Metric.GoalDays, goal = 1,
            icon = AchievementIcon.Ring, accentIndex = 0,
        ),
        AchievementDef(
            key = "goal_seven",
            title = "Seven Goals Met",
            description = "Meet your daily goal on seven days.",
            note = "Consistency, measured on your own terms.",
            metric = Metric.GoalDays, goal = 7,
            icon = AchievementIcon.Ring, accentIndex = 5,
        ),
        AchievementDef(
            key = "goal_thirty",
            title = "Thirty Goals Met",
            description = "Meet your daily goal on thirty days.",
            note = "Thirty days you decided for yourself.",
            metric = Metric.GoalDays, goal = 30,
            icon = AchievementIcon.Compass, accentIndex = 4,
        ),
        AchievementDef(
            key = "own_words",
            title = "In Your Own Words",
            description = "Create your first personal dhikr.",
            note = "The ones you choose yourself tend to stay.",
            metric = Metric.CustomDhikrCreated, goal = 1,
            icon = AchievementIcon.Feather, accentIndex = 2,
        ),
        AchievementDef(
            key = "own_collection",
            title = "Your Own Collection",
            description = "Create your first personal collection.",
            note = "Arranged the way you actually pray.",
            metric = Metric.CollectionsCreated, goal = 1,
            icon = AchievementIcon.Heart, accentIndex = 2,
        ),
        AchievementDef(
            key = "long_sitting",
            title = "A Long Sitting",
            description = "Count 100 in a single unbroken round.",
            note = "Sometimes the quiet lasts a while.",
            metric = Metric.BestSession, goal = 100,
            icon = AchievementIcon.Feather, accentIndex = 5,
        ),
        AchievementDef(
            key = "full_day",
            title = "A Full Day",
            description = "Count 500 in a single day.",
            note = "A day that leaned toward remembrance.",
            metric = Metric.BestDay, goal = 500,
            icon = AchievementIcon.Star, accentIndex = 0,
        ),
        AchievementDef(
            key = "thirty_days_present",
            title = "Thirty Days Present",
            description = "Remember on thirty separate days.",
            note = "They did not have to be in a row to count.",
            metric = Metric.ActiveDays, goal = 30,
            icon = AchievementIcon.Compass, accentIndex = 4,
        ),
    )

    private val byKey: Map<String, AchievementDef> = all.associateBy { it.key }

    fun find(key: String): AchievementDef? = byKey[key]

    /** Keys whose goal the snapshot now satisfies. The repository decides which are new. */
    fun satisfied(snapshot: GamificationSnapshot): List<String> =
        all.filter { snapshot.valueOf(it.metric) >= it.goal }.map { it.key }

    fun statuses(
        snapshot: GamificationSnapshot,
        unlockedAt: Map<String, Long>,
    ): List<AchievementStatus> = all
        .map { def ->
            AchievementStatus(
                def = def,
                progress = snapshot.valueOf(def.metric).coerceAtMost(def.goal),
                unlockedAt = unlockedAt[def.key],
            )
        }
        .sortedWith(
            compareByDescending<AchievementStatus> { it.isUnlocked }
                .thenByDescending { it.fraction }
        )
}
