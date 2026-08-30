package com.lillah.dhikr.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.StreakInfo
import com.lillah.dhikr.ui.screens.home.HomeScreen
import com.lillah.dhikr.ui.screens.home.HomeUiState
import com.lillah.dhikr.ui.theme.DhikrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Renders the real screen rather than asserting on state in isolation. These catch the class of
 * mistake that unit tests on a ViewModel cannot: a composable that throws on an empty list, a
 * missing CompositionLocal, a layout that cannot measure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val subhanAllah = Dhikr(
        id = 1,
        name = "SubhanAllah",
        arabic = "سُبْحَانَ اللَّهِ",
        transliteration = "SubhanAllah",
        meaning = "Glory be to Allah",
        targetCount = 33,
        currentCount = 12,
    )

    private fun render(
        state: HomeUiState,
        onCount: () -> Unit = {},
        onUndo: () -> Unit = {},
    ) {
        compose.setContent {
            DhikrTheme(animateColors = false) {
                HomeScreen(
                    state = state,
                    onCount = onCount,
                    onUndo = onUndo,
                    onReset = {},
                    onSelectDhikr = {},
                    onSetTarget = {},
                    onAddDhikr = {},
                    onOpenCollection = {},
                    onOpenProgress = {},
                    onDismissMilestone = {},
                    now = LocalTime.of(9, 0),
                    today = LocalDate.of(2026, 3, 4),
                )
            }
        }
    }

    @Test
    fun `renders the counter with the active dhikr`() {
        render(
            HomeUiState(
                isLoading = false,
                activeDhikr = subhanAllah,
                displayCount = 12,
                quickPicks = listOf(subhanAllah),
                todayTotal = 42,
                streak = StreakInfo(current = 3, best = 5, activeToday = true),
            )
        )

        // Three matches by design: the headline, the transliteration under it, and the
        // quick-pick chip (whose merged node also carries the name).
        compose.onAllNodesWithText("SubhanAllah").assertCountEquals(3)
        // The chip's own label; its merged node carries the name too, hence three matches above.
        compose.onNodeWithText("×33").assertExists()
        compose.onNodeWithText("Glory be to Allah").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("of 33").assertIsDisplayed()
        compose.onNodeWithText("Good morning").assertIsDisplayed()
        compose.onNodeWithText("3 days").assertIsDisplayed()
    }

    @Test
    fun `tapping the counter reports one count`() {
        var counts = 0
        render(
            HomeUiState(
                isLoading = false,
                activeDhikr = subhanAllah,
                displayCount = 12,
                quickPicks = listOf(subhanAllah),
            ),
            onCount = { counts++ },
        )

        compose.onAllNodesWithContentDescription(
            label = "Tasbih counter",
            substring = true,
        )[0].performClick()

        assertEquals(1, counts)
    }

    @Test
    fun `an empty state renders without an active dhikr`() {
        render(HomeUiState(isLoading = false))
        compose.onNodeWithText("Choose a dhikr to begin").assertIsDisplayed()
    }

    @Test
    fun `a met goal is described as met rather than as a total`() {
        render(
            HomeUiState(
                isLoading = false,
                activeDhikr = subhanAllah,
                displayCount = 33,
                todayTotal = 150,
            )
        )
        compose.onNodeWithText("150 of 100 today").assertIsDisplayed()
        compose.onNodeWithText("Today's goal is met. Anything more is a gift.").assertExists()
    }
}
