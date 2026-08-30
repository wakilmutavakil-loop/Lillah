package com.lillah.dhikr.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.data.prefs.UserSettings
import com.lillah.dhikr.domain.gamification.AchievementDef
import com.lillah.dhikr.domain.gamification.GrowthState
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.domain.model.DayPoint
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.StreakInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val activeDhikr: Dhikr? = null,
    val displayCount: Int = 0,
    val quickPicks: List<Dhikr> = emptyList(),
    val activeDhikrToday: Int = 0,
    val todayTotal: Int = 0,
    val streak: StreakInfo = StreakInfo(),
    val growth: GrowthState = GrowthState(),
    val recentDays: List<DayPoint> = emptyList(),
    val timeSuggestion: CollectionProgress? = null,
    val celebrationKey: Int = 0,
    val pendingMilestone: AchievementDef? = null,
) {
    val goalFraction: Float
        get() = (todayTotal.toFloat() / settings.dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val goalMet: Boolean get() = todayTotal >= settings.dailyGoal
}

@Immutable
private data class DailyBundle(
    val todayTotal: Int,
    val streak: StreakInfo,
    val growth: GrowthState,
    val recentDays: List<DayPoint>,
    val activeCounts: Map<Long, Int>,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val dhikrRepository = container.dhikrRepository
    private val statsRepository = container.statsRepository
    private val gamification = container.gamificationRepository
    private val settingsRepository = container.settingsRepository
    private val clock = container.clock

    /**
     * The count shown while a write is in flight.
     *
     * Taps are answered on the frame they happen; the database confirms a moment later. Without
     * this the ring would lag behind a fast thumb, which is exactly the interaction the whole
     * screen is built around.
     */
    private val optimisticCount = MutableStateFlow<Int?>(null)
    private val celebrations = MutableStateFlow(0)
    private val dismissedMilestones = MutableStateFlow<Set<String>>(emptySet())

    /** Writes are drained by one consumer, so taps land in the order they were made. */
    private val tapQueue = Channel<Int>(capacity = Channel.UNLIMITED)

    /** Milestone evaluation is comparatively expensive, so it trails a burst of taps. */
    private val evaluationTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val settingsFlow = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val activeDhikrFlow: StateFlow<Dhikr?> = settingsFlow
        .map { it.activeDhikrId }
        .distinctUntilChanged()
        .flatMapLatest { id -> dhikrRepository.observeDhikr(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val dailyBundle = combine(
        statsRepository.observeTodayTotal(),
        statsRepository.observeStreak(),
        statsRepository.observeGrowth(),
        statsRepository.observeRecentDays(14),
        statsRepository.observeDhikrTodayCounts(),
    ) { total, streak, growth, recent, counts ->
        DailyBundle(total, streak, growth, recent, counts)
    }

    private val suggestionFlow = dhikrRepository.observeCollectionProgress()
        .map { collections -> pickSuggestion(collections) }

    private val milestoneFlow = combine(
        gamification.observePendingCelebrations(),
        dismissedMilestones,
    ) { pending, dismissed ->
        pending.firstOrNull { it.key !in dismissed }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(settingsFlow, activeDhikrFlow, optimisticCount) { settings, dhikr, optimistic ->
            Triple(settings, dhikr, optimistic)
        },
        dhikrRepository.observeAll(),
        dailyBundle,
        suggestionFlow,
        combine(milestoneFlow, celebrations) { milestone, key -> milestone to key },
    ) { core, allDhikr, daily, suggestion, milestoneAndKey ->
        val (settings, dhikr, optimistic) = core
        val (milestone, celebrationKey) = milestoneAndKey

        HomeUiState(
            isLoading = false,
            settings = settings,
            activeDhikr = dhikr,
            displayCount = optimistic ?: dhikr?.currentCount ?: 0,
            quickPicks = orderQuickPicks(allDhikr, dhikr?.id),
            activeDhikrToday = dhikr?.let { daily.activeCounts[it.id] } ?: 0,
            todayTotal = daily.todayTotal,
            streak = daily.streak,
            growth = daily.growth,
            recentDays = daily.recentDays,
            timeSuggestion = suggestion,
            celebrationKey = celebrationKey,
            pendingMilestone = milestone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Keeps a valid dhikr selected: on a first run once seeding lands, and again if the
        // selected one is deleted. Reading settingsFlow.value here instead would race DataStore
        // and reset the user's choice on every launch.
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                dhikrRepository.observeAll(),
            ) { settings, all -> settings.activeDhikrId to all }
                .collect { (activeId, all) ->
                    if (all.isNotEmpty() && all.none { it.id == activeId }) {
                        settingsRepository.setActiveDhikr(all.first().id)
                    }
                }
        }

        viewModelScope.launch {
            for (delta in tapQueue) {
                val id = uiState.value.activeDhikr?.id ?: continue
                if (id <= 0) continue
                val result = if (delta > 0) {
                    dhikrRepository.increment(id, delta)
                } else {
                    dhikrRepository.decrement(id)
                }
                if (result != null) optimisticCount.value = result.currentCount
                evaluationTrigger.tryEmit(Unit)
            }
        }

        viewModelScope.launch {
            evaluationTrigger.debounce(450).collect {
                gamification.refresh(settingsRepository.settings.first().dailyGoal)
            }
        }

        // Drop the optimistic override once the database has caught up with it, so a change made
        // elsewhere (a reset from inside a collection, say) is not masked by a stale local value.
        viewModelScope.launch {
            combine(activeDhikrFlow, optimisticCount) { dhikr, optimistic ->
                dhikr?.currentCount to optimistic
            }.collect { (stored, optimistic) ->
                if (optimistic != null && stored == optimistic) optimisticCount.value = null
            }
        }

        // Hardware volume keys, when the user has opted into them.
        viewModelScope.launch {
            container.hardwareKeyCounts.collect { delta ->
                if (uiState.value.settings.countWithVolumeKeys) {
                    if (delta > 0) count() else undo()
                }
            }
        }
    }

    fun count() {
        val state = uiState.value
        val dhikr = state.activeDhikr ?: return
        val target = dhikr.safeTarget
        val current = state.displayCount
        // A finished round stays on screen until the next tap, which starts the next one.
        val next = if (current >= target) 1 else (current + 1).coerceAtMost(target)
        optimisticCount.value = next

        val settings = state.settings
        if (next >= target) {
            celebrations.value += 1
            container.haptics.roundComplete(settings.hapticsEnabled)
            container.sounds.playChime(settings.soundEnabled)
        } else {
            container.haptics.tick(settings.hapticsEnabled)
            container.sounds.playTick(settings.soundEnabled)
        }

        tapQueue.trySend(1)
    }

    fun undo() {
        val state = uiState.value
        state.activeDhikr ?: return
        container.haptics.undo(state.settings.hapticsEnabled)
        optimisticCount.value = (state.displayCount - 1).coerceAtLeast(0)
        tapQueue.trySend(-1)
    }

    fun resetRound() {
        val id = uiState.value.activeDhikr?.id ?: return
        optimisticCount.value = 0
        viewModelScope.launch { dhikrRepository.resetRound(id) }
    }

    fun selectDhikr(id: Long) {
        if (id == uiState.value.activeDhikr?.id) return
        optimisticCount.value = null
        viewModelScope.launch { settingsRepository.setActiveDhikr(id) }
    }

    fun setTarget(target: Int) {
        val dhikr = uiState.value.activeDhikr ?: return
        optimisticCount.value = null
        viewModelScope.launch {
            dhikrRepository.upsert(dhikr.copy(targetCount = target.coerceIn(1, 10_000)))
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal) }
    }

    fun dismissMilestone() {
        val milestone = uiState.value.pendingMilestone ?: return
        dismissedMilestones.value = dismissedMilestones.value + milestone.key
        viewModelScope.launch { gamification.markCelebrated(milestone.key) }
    }

    /** Favourites first, then the rest, with the active dhikr always reachable near the front. */
    private fun orderQuickPicks(all: List<Dhikr>, activeId: Long?): List<Dhikr> {
        if (all.isEmpty()) return emptyList()
        return all.sortedWith(
            compareByDescending<Dhikr> { it.id == activeId }
                .thenByDescending { it.isFavorite }
                .thenBy { it.sortOrder }
        ).take(12)
    }

    /**
     * Offers Morning Adhkar in the morning and Evening Adhkar from mid-afternoon, and stops
     * offering either once it is finished for the day.
     */
    private fun pickSuggestion(collections: List<CollectionProgress>): CollectionProgress? {
        val hour = clock.timeOfDay().hour
        val wanted = when (hour) {
            in 4..10 -> CollectionKind.Morning
            in 15..21 -> CollectionKind.Evening
            else -> null
        } ?: return null
        return collections.firstOrNull { it.collection.kind == wanted && !it.isComplete }
    }
}
