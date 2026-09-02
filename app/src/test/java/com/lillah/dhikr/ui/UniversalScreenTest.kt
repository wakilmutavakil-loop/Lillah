package com.lillah.dhikr.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import com.lillah.dhikr.data.prefs.AccountState
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.RemoteFigures
import com.lillah.dhikr.domain.sync.SyncStatus
import com.lillah.dhikr.ui.screens.universal.PersonalTotals
import com.lillah.dhikr.ui.screens.universal.UniversalScreen
import com.lillah.dhikr.ui.screens.universal.UniversalUiState
import com.lillah.dhikr.ui.theme.DhikrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UniversalScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(state: UniversalUiState) {
        compose.setContent {
            DhikrTheme(animateColors = false) {
                UniversalScreen(
                    state = state,
                    onSignIn = {},
                    onSignOut = {},
                    onSyncNow = {},
                    onDismissMessage = {},
                )
            }
        }
    }

    @Test
    fun `signed in, the board shows the worldwide figure and the user's share`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(deviceId = "d", uid = "u", displayName = "Aisha"),
                syncStatus = SyncStatus(backendConfigured = true, signedIn = true),
                figures = RemoteFigures(
                    globalTotal = 2_000_000_000,
                    globalToday = 4_120_000,
                    participantCount = 51_284,
                    userTotal = 30_000,
                ),
                totals = PersonalTotals(today = 320, week = 2_100, month = 8_400, year = 29_000, allTimeLocal = 30_000),
            )
        )

        compose.onNodeWithText("UNIVERSAL DHIKR").assertIsDisplayed()
        compose.onNodeWithText("Total dhikr worldwide").assertIsDisplayed()
        compose.onNodeWithText("YOUR CONTRIBUTION").assertExists()
        // 30,000 of 2,000,000,000 — the figure the acceptance criteria names.
        compose.scrollTo("0.0015%")
        compose.onNodeWithText("0.0015%").assertIsDisplayed()
        compose.onNodeWithText("of the Universal Dhikr").assertExists()
    }

    @Test
    fun `the share reflects work still queued, so the number never stalls mid-sync`() {
        val state = UniversalUiState(
            backendConfigured = true,
            account = AccountState(uid = "u"),
            syncStatus = SyncStatus(backendConfigured = true, signedIn = true, pendingOperations = 500, pendingTotal = 500),
            figures = RemoteFigures(globalTotal = 1_000_000, userTotal = 10_000),
            totals = PersonalTotals(allTimeLocal = 10_500),
        )
        assertEquals("server total plus what is still queued", 10_500L, state.userContribution)
        render(state)
        compose.scrollTo("Sync pending")
        compose.onNodeWithText("Sync pending").assertIsDisplayed()
    }

    @Test
    fun `signed out, it shows this device's own total and offers sign-in`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                syncStatus = SyncStatus(backendConfigured = true),
                totals = PersonalTotals(allTimeLocal = 12_500),
                availableMethods = listOf(AuthMethod.Google, AuthMethod.Facebook),
            )
        )

        compose.onNodeWithText("Your total dhikr").assertExists()
        compose.scrollTo("Join the Universal Dhikr")
        compose.onNodeWithText("Join the Universal Dhikr").assertIsDisplayed()
        compose.onNodeWithText("Continue with Google").assertIsDisplayed()
        compose.onNodeWithText("Continue with Facebook").assertIsDisplayed()
    }

    @Test
    fun `with no backend it explains itself instead of showing an error`() {
        render(
            UniversalUiState(
                backendConfigured = false,
                totals = PersonalTotals(today = 33, allTimeLocal = 900),
            )
        )

        compose.onNodeWithText("Not connected").assertIsDisplayed()
        compose.onNodeWithText("Your total dhikr").assertExists()
        compose.scrollTo("Saved on this device")
        compose.onNodeWithText("Saved on this device").assertIsDisplayed()
    }

    @Test
    fun `a signed-out user's contribution is what this device counted`() {
        val state = UniversalUiState(totals = PersonalTotals(allTimeLocal = 25_000))
        assertEquals(25_000L, state.userContribution)
    }

    @Test
    fun `every personal period is on screen`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                totals = PersonalTotals(today = 33, week = 210, month = 900, year = 11_000, allTimeLocal = 25_000),
            )
        )
        listOf("Today", "This week", "This month", "This year", "All time on this device")
            .forEach { label ->
                compose.scrollTo(label)
                compose.onNodeWithText(label).assertIsDisplayed()
            }
    }
}

/** The board is a lazy list; anything below the fold has to be scrolled to before it exists. */
private fun ComposeContentTestRule.scrollTo(text: String) {
    onNode(hasScrollAction()).performScrollToNode(hasText(text))
}
