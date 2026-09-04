package com.lillah.dhikr.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.lillah.dhikr.data.guide.GuideContent
import com.lillah.dhikr.data.prefs.UserSettings
import com.lillah.dhikr.domain.gamification.AchievementCatalog
import com.lillah.dhikr.domain.gamification.GamificationSnapshot
import com.lillah.dhikr.domain.gamification.GrowthState
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.domain.model.CoverArt
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.DhikrCollection
import com.lillah.dhikr.domain.model.DhikrProgress
import com.lillah.dhikr.domain.model.StreakInfo
import com.lillah.dhikr.ui.screens.collections.CollectionDetailUiState
import com.lillah.dhikr.ui.screens.collections.CollectionDetailScreen
import com.lillah.dhikr.ui.screens.collections.CollectionsScreen
import com.lillah.dhikr.ui.screens.collections.CollectionsUiState
import com.lillah.dhikr.ui.screens.guide.GuideArticleScreen
import com.lillah.dhikr.ui.screens.guide.GuideScreen
import com.lillah.dhikr.ui.screens.progress.ProgressRange
import com.lillah.dhikr.ui.screens.progress.ProgressScreen
import com.lillah.dhikr.ui.screens.progress.ProgressUiState
import com.lillah.dhikr.ui.screens.settings.SettingsScreen
import com.lillah.dhikr.ui.screens.settings.SettingsUiState
import com.lillah.dhikr.ui.theme.DhikrTheme
import com.lillah.dhikr.ui.theme.ThemePalette
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke tests that every screen composes and lays out, including the states that are easy to get
 * wrong: an empty list, a fully complete collection, and every palette in turn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreensRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private val morning = DhikrCollection(
        id = 1,
        name = "Morning Adhkar",
        arabicName = "أذكار الصباح",
        kind = CollectionKind.Morning,
        artwork = CoverArt.Sunrise,
    )

    @Test
    fun `collections screen renders featured covers`() {
        compose.setContent {
            DhikrTheme(animateColors = false) {
                CollectionsScreen(
                    state = CollectionsUiState(
                        isLoading = false,
                        featured = listOf(
                            CollectionProgress(morning, itemCount = 16, completedToday = 4, totalToday = 60)
                        ),
                        others = listOf(
                            CollectionProgress(
                                morning.copy(id = 3, name = "Everyday Tasbih", kind = CollectionKind.Essentials),
                                itemCount = 11, completedToday = 11, totalToday = 400,
                            )
                        ),
                        favorites = listOf(Dhikr(id = 1, name = "SubhanAllah", isFavorite = true)),
                        totalAdhkar = 27,
                    ),
                    onOpenCollection = {},
                    onCreateCollection = {},
                    onManageAdhkar = {},
                    onSelectDhikr = {},
                )
            }
        }

        compose.onNodeWithText("Morning Adhkar").assertIsDisplayed()
        compose.onNodeWithText("4 of 16 today").assertIsDisplayed()
        compose.onNodeWithText("27 adhkar across your collections").assertIsDisplayed()
    }

    @Test
    fun `collections screen survives having nothing to show`() {
        compose.setContent {
            DhikrTheme(animateColors = false) {
                CollectionsScreen(
                    state = CollectionsUiState(isLoading = false),
                    onOpenCollection = {},
                    onCreateCollection = {},
                    onManageAdhkar = {},
                    onSelectDhikr = {},
                )
            }
        }
        compose.onNodeWithText("No collections yet").assertIsDisplayed()
    }

    @Test
    fun `a finished collection says so`() {
        val items = List(3) { index ->
            DhikrProgress(
                dhikr = Dhikr(id = index + 1L, name = "Item $index", targetCount = 3),
                countToday = 3,
            )
        }
        compose.setContent {
            DhikrTheme(animateColors = false) {
                CollectionDetailScreen(
                    state = CollectionDetailUiState(
                        isLoading = false,
                        collection = morning,
                        items = items,
                    ),
                    onBack = {}, onOpenDhikr = {}, onCountOne = {}, onUndoOne = {},
                    onPickCover = {}, onClearCover = {}, onEditCollection = {},
                    onAddDhikr = {},
                )
            }
        }
        compose.onNodeWithText("Complete for today").assertIsDisplayed()
    }

    @Test
    fun `progress screen renders all three ranges`() {
        val state = ProgressUiState(
            isLoading = false,
            todayTotal = 120,
            dailyGoal = 100,
            streak = StreakInfo(current = 4, best = 9, activeToday = true),
            growth = GrowthState.forTotal(1_500),
            lifetimeTotal = 1_500,
            activeDays = 22,
            achievements = AchievementCatalog.statuses(
                GamificationSnapshot(lifetimeTotal = 1_500, currentStreak = 4),
                mapOf("first_remembrance" to 1L, "hundred" to 2L, "thousand" to 3L),
            ),
        )

        val range = mutableStateOf(ProgressRange.Day)
        compose.setContent {
            DhikrTheme(animateColors = false) {
                ProgressScreen(state = state, range = range.value, onSelectRange = {})
            }
        }

        compose.runOnIdle { range.value = ProgressRange.Day }
        compose.onNodeWithText("By dhikr").assertIsDisplayed()

        compose.runOnIdle { range.value = ProgressRange.Week }
        compose.onNodeWithText("this week").assertIsDisplayed()

        compose.runOnIdle { range.value = ProgressRange.Month }
        compose.onNodeWithText("days present").assertIsDisplayed()

        // The garden card and milestone grid live below the fold, so reaching them also proves
        // the lazy list composes them rather than throwing on the way down.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Sapling"))
        compose.onNodeWithText("Sapling").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("One Thousand"))
        compose.onNodeWithText("One Thousand").assertIsDisplayed()
    }

    @Test
    fun `progress screen renders with no history at all`() {
        compose.setContent {
            DhikrTheme(animateColors = false) {
                ProgressScreen(
                    state = ProgressUiState(isLoading = false),
                    range = ProgressRange.Day,
                    onSelectRange = {},
                )
            }
        }
        compose.onNodeWithText("Nothing counted yet today.").assertIsDisplayed()
    }

    @Test
    fun `guide lists its articles and opens one`() {
        var opened: String? = null
        compose.setContent {
            DhikrTheme(animateColors = false) {
                GuideScreen(onOpenArticle = { opened = it })
            }
        }
        compose.onNodeWithText("Guidebook").assertIsDisplayed()
        compose.onNodeWithText("Using this app").assertIsDisplayed()
        compose.onNodeWithText("Using this app").performClick()
        assertEquals("using-the-app", opened)
    }

    @Test
    fun `every guide article renders every block type`() {
        val article = mutableStateOf(GuideContent.articles.first())
        compose.setContent {
            DhikrTheme(animateColors = false) {
                GuideArticleScreen(article = article.value, onBack = {})
            }
        }

        GuideContent.articles.forEach { current ->
            compose.runOnIdle { article.value = current }
            compose.onNodeWithText(current.title).assertIsDisplayed()
            compose.onNodeWithText(current.subtitle).assertIsDisplayed()
        }
    }

    @Test
    fun `settings renders in every palette`() {
        val palette = mutableStateOf(ThemePalette.Default)
        compose.setContent {
            DhikrTheme(palette = palette.value, animateColors = false) {
                SettingsScreen(
                    state = SettingsUiState(
                        settings = UserSettings(palette = palette.value),
                        lifetimeTotal = 4_210,
                        adhkarCount = 51,
                        collectionCount = 4,
                    ),
                    isDark = false,
                    onSelectPalette = {}, onSelectMode = {}, onToggleHaptics = {},
                    onToggleSound = {}, onToggleKeepScreenOn = {}, onToggleVolumeKeys = {},
                    onToggleArabic = {}, onToggleTransliteration = {}, onToggleMeaning = {},
                    onSetDailyGoal = {}, onManageAdhkar = {}, onRestoreDefaults = {},
                )
            }
        }

        // Swapping the palette re-derives the whole Material scheme; the assertion is simply
        // that every one of them composes and lays out.
        ThemePalette.entries.forEach { current ->
            compose.runOnIdle { palette.value = current }
            compose.onNodeWithText("4,210 remembrances so far").assertExists()
        }
        compose.onNodeWithText(ThemePalette.Copilot.tagline).assertExists()
    }
}
