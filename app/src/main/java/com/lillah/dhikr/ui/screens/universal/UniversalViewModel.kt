package com.lillah.dhikr.ui.screens.universal

import android.app.Activity
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.data.backend.SignInCancelled
import com.lillah.dhikr.data.prefs.AccountState
import com.lillah.dhikr.data.repository.userMessage
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.RemoteFigures
import com.lillah.dhikr.domain.sync.SyncStatus
import com.lillah.dhikr.domain.sync.contributionPercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class PersonalTotals(
    val today: Int = 0,
    val week: Int = 0,
    val month: Int = 0,
    val year: Int = 0,
    val allTimeLocal: Long = 0,
)

/** Why the app is asking the user to go online. */
enum class ConnectReason { NeverConnected, ContributionWaiting, FiguresStale }

@Immutable
data class ConnectPrompt(
    val reason: ConnectReason,
    val pendingTotal: Long,
    val lastSyncAt: Long?,
)

@Immutable
data class UniversalUiState(
    val backendConfigured: Boolean = false,
    val account: AccountState = AccountState(),
    val syncStatus: SyncStatus = SyncStatus(),
    val figures: RemoteFigures? = null,
    val totals: PersonalTotals = PersonalTotals(),
    val availableMethods: List<AuthMethod> = emptyList(),
    val signingIn: Boolean = false,
    val message: String? = null,
    val profileName: String? = null,
    val connectPrompt: ConnectPrompt? = null,
) {
    /**
     * The user's contribution, taken from this device.
     *
     * Counting is stored locally and the device is the authority on it; the network is only ever
     * used to add that figure to the worldwide count and to read the worldwide count back. So this
     * is the local lifetime total, always — it cannot be behind, and it cannot be lost by a
     * failed sync.
     */
    val userContribution: Long get() = totals.allTimeLocal

    /** How much of [userContribution] the world count has not been told about yet. */
    val awaitingUpload: Long get() = syncStatus.pendingTotal.coerceAtLeast(0)

    val globalTotal: Long get() = figures?.globalTotal ?: 0

    /** Always derived, never stored: both operands move independently. */
    val contributionPercent: Double
        get() = contributionPercent(userContribution, globalTotal)

    /** True once a figure has actually been read back. Zero is a real answer, not a missing one. */
    val hasGlobalFigures: Boolean get() = figures != null

    /** Why the worldwide figure is not on screen, phrased for the person looking at it. */
    val globalPlaceholder: String
        get() = when {
            !backendConfigured ->
                "This build has no cloud attached. Everything below is counted and kept on " +
                    "your device."
            !account.isSignedIn ->
                "Sign in below to add your dhikr to the worldwide count."
            syncStatus.lastError != null -> syncStatus.lastError
            syncStatus.syncing -> "Reading the worldwide count…"
            else -> "Connect to read the worldwide count."
        }
}

class UniversalViewModel(private val container: AppContainer) : ViewModel() {

    private val syncRepository = container.syncRepository
    private val accountRepository = container.accountRepository
    private val statsRepository = container.statsRepository
    private val authGateway = container.authGateway

    private val profileRepository = container.profileRepository
    private val dhikrRepository = container.dhikrRepository

    private val signingIn = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    /** Set when the user dismisses the prompt, so it stays quiet for a day. */
    private val promptSnoozedAt = MutableStateFlow(0L)

    private val totals = combine(
        statsRepository.observeTodayTotal(),
        statsRepository.observeWeekTotal(),
        statsRepository.observeMonthTotal(),
        statsRepository.observeYearTotal(),
        statsRepository.observeLifetimeTotal(),
    ) { today, week, month, year, lifetime ->
        PersonalTotals(today, week, month, year, lifetime)
    }

    val uiState: StateFlow<UniversalUiState> = combine(
        accountRepository.state,
        syncRepository.status,
        syncRepository.cachedFigures,
        totals,
        combine(signingIn, message, promptSnoozedAt, profileRepository.activeProfile) {
                busy, note, snoozed, profile ->
            Quad(busy, note, snoozed, profile?.displayName)
        },
    ) { account, status, figures, personal, extras ->
        UniversalUiState(
            backendConfigured = container.backend.isConfigured,
            account = account,
            syncStatus = status,
            figures = figures,
            totals = personal,
            availableMethods = authGateway.availableMethods,
            signingIn = extras.signingIn,
            message = extras.message,
            profileName = extras.profileName,
            connectPrompt = connectPrompt(status, figures, account.lastSyncAt, extras.snoozedAt),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UniversalUiState())

    private data class Quad(
        val signingIn: Boolean,
        val message: String?,
        val snoozedAt: Long,
        val profileName: String?,
    )

    /**
     * Decides whether to ask the user to go online.
     *
     * The app is usable indefinitely without a connection, so this is an invitation rather than a
     * warning, and it only appears when connecting would actually achieve something: there is a
     * contribution to add, or the worldwide figure on screen has gone stale. Dismissing it buys a
     * day of quiet.
     */
    private fun connectPrompt(
        status: SyncStatus,
        figures: RemoteFigures?,
        lastSyncAt: Long?,
        snoozedAt: Long,
    ): ConnectPrompt? {
        if (!status.backendConfigured || !status.signedIn || status.syncing) return null
        val now = container.clock.nowMillis()
        if (now - snoozedAt < SNOOZE_MILLIS) return null

        val reason = when {
            lastSyncAt == null -> ConnectReason.NeverConnected
            status.hasPending -> ConnectReason.ContributionWaiting
            now - (figures?.updatedAt ?: 0) > STALE_MILLIS -> ConnectReason.FiguresStale
            else -> return null
        }
        return ConnectPrompt(reason, status.pendingTotal, lastSyncAt)
    }

    init {
        // Opening the tab reads the worldwide figure. Without this the board only ever filled in
        // as a side effect of an upload, so a user with nothing to contribute saw nothing at all.
        viewModelScope.launch { syncRepository.refreshFigures() }
    }

    /**
     * Signs in, then attaches whatever this device already counted to the new account before the
     * first sync runs — so a user who upgrades and signs in sees their existing total, not zero.
     */
    fun signIn(activity: Activity, method: AuthMethod) {
        if (signingIn.value) return
        viewModelScope.launch {
            signingIn.value = true
            message.value = null
            authGateway.signIn(activity, method)
                .onSuccess { user ->
                    // Resolve the local profile first. The first account to sign in adopts the
                    // device profile, so an upgrading user finds their existing counting already
                    // there; a second account gets a profile of its own and is seeded fresh.
                    val resolution = profileRepository.onSignedIn(user)
                    accountRepository.setSignedIn(
                        uid = user.uid,
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoUrl,
                        method = user.method ?: method,
                    )
                    dhikrRepository.seedIfEmpty(resolution.profileId)
                    // Nothing to claim or migrate: the first upload is this device's whole
                    // lifetime total, so existing history joins the world count on its own.
                    syncRepository.syncNow()
                }
                .onFailure { error ->
                    message.value = if (error is SignInCancelled) null else error.userMessage()
                }
            signingIn.value = false
        }
    }

    /** Ends the session only. Counting history, queued work and settings are untouched. */
    fun signOut() {
        viewModelScope.launch {
            authGateway.signOut()
            accountRepository.clearSignedIn()
            message.value = "Signed out. Your dhikr stay on this device."
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncRepository.syncNow()
                .onFailure { message.value = it.userMessage() }
                .onSuccess { message.value = null }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    /** Quiets the connect prompt for a day. It never disables it permanently. */
    fun snoozeConnectPrompt() {
        val now = container.clock.nowMillis()
        promptSnoozedAt.value = now
        viewModelScope.launch { accountRepository.recordConnectPrompt(now) }
    }

    private companion object {
        const val SNOOZE_MILLIS = 24 * 60 * 60 * 1000L
        const val STALE_MILLIS = 12 * 60 * 60 * 1000L
    }
}
