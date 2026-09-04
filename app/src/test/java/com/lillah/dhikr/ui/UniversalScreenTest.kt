package com.lillah.dhikr.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lillah.dhikr.data.prefs.AccountState
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.RemoteFigures
import com.lillah.dhikr.domain.sync.SyncStatus
import com.lillah.dhikr.ui.screens.universal.ConnectPrompt
import com.lillah.dhikr.ui.screens.universal.ConnectReason
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
                    onSnoozePrompt = {},
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
    fun `an unread today figure shows as unknown, never as zero`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(deviceId = "d", uid = "u", displayName = "Aisha"),
                syncStatus = SyncStatus(backendConfigured = true, signedIn = true),
                figures = RemoteFigures(
                    globalTotal = 2_000_000_000,
                    globalToday = null,
                    participantCount = 51_284,
                ),
                totals = PersonalTotals(allTimeLocal = 30_000),
            )
        )

        // The lifetime total is real and shown; today's could not be read, and saying "0" would
        // claim nobody counted today.
        compose.scrollTo("worldwide today")
        compose.onNodeWithText("\u2014").assertIsDisplayed()
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
        assertEquals("the device is the authority on your own total", 10_500L, state.userContribution)
        assertEquals("and it says how much has not reached the world yet", 500L, state.awaitingUpload)
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

    @Test
    fun `the connect prompt appears with what is waiting and can be dismissed`() {
        var connected = 0
        var snoozed = 0
        compose.setContent {
            DhikrTheme(animateColors = false) {
                UniversalScreen(
                    state = UniversalUiState(
                        backendConfigured = true,
                        account = AccountState(uid = "u"),
                        syncStatus = SyncStatus(
                            backendConfigured = true,
                            signedIn = true,
                            pendingOperations = 3,
                            pendingTotal = 1_240,
                        ),
                        figures = RemoteFigures(globalTotal = 5_000_000, userTotal = 10_000),
                        totals = PersonalTotals(allTimeLocal = 11_240),
                        connectPrompt = ConnectPrompt(
                            reason = ConnectReason.ContributionWaiting,
                            pendingTotal = 1_240,
                            lastSyncAt = 1_700_000_000_000,
                        ),
                    ),
                    onSignIn = {},
                    onSignOut = {},
                    onSyncNow = { connected++ },
                    onDismissMessage = {},
                    onSnoozePrompt = { snoozed++ },
                )
            }
        }

        compose.onNodeWithText("You have dhikr waiting to join the world").assertIsDisplayed()
        compose.onNodeWithText("Connect now").assertIsDisplayed().performClick()
        assertEquals(1, connected)
        compose.onNodeWithText("Later").performClick()
        assertEquals(1, snoozed)
    }

    @Test
    fun `a first-time user is invited to connect rather than warned`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(uid = "u"),
                syncStatus = SyncStatus(backendConfigured = true, signedIn = true),
                totals = PersonalTotals(allTimeLocal = 25_000),
                connectPrompt = ConnectPrompt(ConnectReason.NeverConnected, 25_000, null),
            )
        )
        compose.onNodeWithText("Add your dhikr to the world count").assertIsDisplayed()
    }

    @Test
    fun `no prompt is shown when there is nothing to gain by connecting`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(uid = "u"),
                syncStatus = SyncStatus(backendConfigured = true, signedIn = true),
                figures = RemoteFigures(globalTotal = 5_000_000, userTotal = 900),
                totals = PersonalTotals(allTimeLocal = 900),
                connectPrompt = null,
            )
        )
        compose.onAllNodesWithText("Connect now").assertCountEquals(0)
    }

    @Test
    fun `the board says how much has not reached the world yet`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(uid = "u"),
                syncStatus = SyncStatus(
                    backendConfigured = true,
                    signedIn = true,
                    pendingOperations = 2,
                    pendingTotal = 500,
                ),
                figures = RemoteFigures(globalTotal = 1_000_000, userTotal = 10_000),
                totals = PersonalTotals(allTimeLocal = 10_500),
            )
        )
        compose.scrollTo("500 of these have not joined the world count yet")
        compose.onNodeWithText("500 of these have not joined the world count yet")
            .assertIsDisplayed()
    }

    @Test
    fun `signed in with no figures yet never tells you to sign in`() {
        // Exactly the state a user reported: signed in, contribution counted, world figure not
        // read back yet. The card used to say "Sign in to join the worldwide count".
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(uid = "u", displayName = "Wakil"),
                syncStatus = SyncStatus(
                    backendConfigured = true,
                    signedIn = true,
                    pendingOperations = 1,
                    pendingTotal = 2_018,
                ),
                figures = null,
                totals = PersonalTotals(allTimeLocal = 2_018),
            )
        )

        compose.onAllNodesWithText("Sign in below to add your dhikr to the worldwide count.")
            .assertCountEquals(0)
        compose.onNodeWithText("Connect to read the worldwide count.").assertIsDisplayed()
    }

    @Test
    fun `a signed-out user is the one asked to sign in`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                syncStatus = SyncStatus(backendConfigured = true),
                totals = PersonalTotals(allTimeLocal = 900),
            )
        )
        compose.onNodeWithText("Sign in below to add your dhikr to the worldwide count.")
            .assertIsDisplayed()
    }

    @Test
    fun `a refused database says what to do about it`() {
        val refusal = "The database is not accepting writes yet. Publish the security rules in " +
            "the Firebase console (Firestore Database → Rules → Publish), then try again."
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(uid = "u"),
                syncStatus = SyncStatus(
                    backendConfigured = true,
                    signedIn = true,
                    pendingOperations = 1,
                    pendingTotal = 2_018,
                    lastError = refusal,
                ),
                totals = PersonalTotals(allTimeLocal = 2_018),
            )
        )

        // On the world card, in place of the missing figure...
        compose.onNodeWithText(refusal).assertIsDisplayed()
        // ...and again on the sync card, where the user looks for status.
        compose.scrollTo("Could not connect")
        compose.onNodeWithText("Could not connect").assertIsDisplayed()
    }

    @Test
    fun `a worldwide total of zero is shown as a number, not as missing data`() {
        render(
            UniversalUiState(
                backendConfigured = true,
                account = AccountState(uid = "u"),
                syncStatus = SyncStatus(backendConfigured = true, signedIn = true),
                figures = RemoteFigures(globalTotal = 0, participantCount = 1),
                totals = PersonalTotals(allTimeLocal = 0),
            )
        )
        compose.onNodeWithText("Total dhikr worldwide").assertIsDisplayed()
    }
}


/** The board is a lazy list; anything below the fold has to be scrolled to before it exists. */
private fun ComposeContentTestRule.scrollTo(text: String) {
    onNode(hasScrollAction()).performScrollToNode(hasText(text))
}
