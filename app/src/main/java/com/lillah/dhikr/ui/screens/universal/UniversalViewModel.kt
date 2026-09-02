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
) {
    /**
     * The user's contribution to the worldwide figure.
     *
     * Signed in, the account total is whatever the server has folded in, plus anything still
     * queued on this device — so the number never stalls while a sync is pending, and a second
     * device signing into the same account shows the account's total rather than its own.
     * Signed out, it is simply what this device has counted.
     */
    val userContribution: Long
        get() = when {
            account.isSignedIn && figures != null ->
                figures.userTotal + syncStatus.pendingTotal
            else -> totals.allTimeLocal
        }

    val globalTotal: Long get() = figures?.globalTotal ?: 0

    /** Always derived, never stored: both operands move independently. */
    val contributionPercent: Double
        get() = contributionPercent(userContribution, globalTotal)

    val hasGlobalFigures: Boolean get() = figures != null && globalTotal > 0
}

class UniversalViewModel(private val container: AppContainer) : ViewModel() {

    private val syncRepository = container.syncRepository
    private val accountRepository = container.accountRepository
    private val statsRepository = container.statsRepository
    private val authGateway = container.authGateway

    private val signingIn = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

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
        combine(signingIn, message) { busy, note -> busy to note },
    ) { account, status, figures, personal, (busy, note) ->
        UniversalUiState(
            backendConfigured = container.backend.isConfigured,
            account = account,
            syncStatus = status,
            figures = figures,
            totals = personal,
            availableMethods = authGateway.availableMethods,
            signingIn = busy,
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UniversalUiState())

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
                    accountRepository.setSignedIn(
                        uid = user.uid,
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoUrl,
                        method = user.method ?: method,
                    )
                    syncRepository.registerUser(user)
                    syncRepository.claimExistingHistory()
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
}
