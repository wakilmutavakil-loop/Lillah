package com.lillah.dhikr.ui.screens.collections

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.domain.model.DhikrCollection
import com.lillah.dhikr.domain.model.DhikrProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CollectionDetailUiState(
    val isLoading: Boolean = true,
    val collection: DhikrCollection? = null,
    val items: List<DhikrProgress> = emptyList(),
    val savingCover: Boolean = false,
) {
    val completed: Int get() = items.count { it.isCompleteToday }
    val fraction: Float
        get() = if (items.isEmpty()) 0f else completed.toFloat() / items.size
    val isComplete: Boolean get() = items.isNotEmpty() && completed == items.size
    val countedToday: Int get() = items.sumOf { it.countToday }
}

class CollectionDetailViewModel(
    private val container: AppContainer,
    private val collectionId: Long,
) : ViewModel() {

    private val dhikrRepository = container.dhikrRepository
    private val statsRepository = container.statsRepository
    private val settingsRepository = container.settingsRepository
    private val coverStore = container.coverImageStore

    private val savingCover = MutableStateFlow(false)

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        dhikrRepository.observeCollection(collectionId),
        dhikrRepository.observeByCollection(collectionId),
        statsRepository.observeDhikrTodayCounts(),
        savingCover,
    ) { collection, items, counts, saving ->
        CollectionDetailUiState(
            isLoading = false,
            collection = collection,
            items = items.map { DhikrProgress(it, counts[it.id] ?: 0) },
            savingCover = saving,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CollectionDetailUiState(),
    )

    /** Counts one repetition without leaving the list — the natural gesture for a "say once" item. */
    fun countOne(dhikrId: Long) {
        viewModelScope.launch {
            // Read the stored settings rather than a cached default: crediting a "goal met" day
            // against the wrong goal would be recorded permanently.
            val current = settingsRepository.settings.first()
            val result = dhikrRepository.increment(dhikrId)
            if (result?.roundCompleted == true) {
                container.haptics.roundComplete(current.hapticsEnabled)
                container.sounds.playChime(current.soundEnabled)
            } else {
                container.haptics.tick(current.hapticsEnabled)
                container.sounds.playTick(current.soundEnabled)
            }
            container.gamificationRepository.refresh(current.dailyGoal)
        }
    }

    fun undoOne(dhikrId: Long) {
        viewModelScope.launch {
            container.haptics.undo(settingsRepository.settings.first().hapticsEnabled)
            dhikrRepository.decrement(dhikrId)
        }
    }

    fun open(dhikrId: Long, onOpened: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setActiveDhikr(dhikrId)
            onOpened()
        }
    }

    fun chooseCover(uri: Uri) {
        viewModelScope.launch {
            savingCover.value = true
            val previous = uiState.value.collection?.coverImagePath
            val path = coverStore.save(collectionId, uri)
            if (path != null) {
                dhikrRepository.setCollectionCover(collectionId, path)
                // Only remove the old file once the new one is safely recorded.
                coverStore.delete(previous)
            }
            savingCover.value = false
        }
    }

    fun clearCover() {
        viewModelScope.launch {
            val previous = uiState.value.collection?.coverImagePath
            dhikrRepository.setCollectionCover(collectionId, null)
            coverStore.delete(previous)
        }
    }

    /** Clears the live rounds in this collection. Today's recorded counts are left untouched. */
    fun resetRounds() {
        viewModelScope.launch {
            uiState.value.items.forEach { dhikrRepository.resetRound(it.dhikr.id) }
        }
    }
}
