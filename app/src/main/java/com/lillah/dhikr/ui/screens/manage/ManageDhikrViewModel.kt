package com.lillah.dhikr.ui.screens.manage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.DhikrCollection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ManageUiState(
    val active: List<Dhikr> = emptyList(),
    val archived: List<Dhikr> = emptyList(),
    val collections: List<DhikrCollection> = emptyList(),
)

class ManageDhikrViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.dhikrRepository

    val uiState: StateFlow<ManageUiState> = combine(
        repository.observeAll(),
        repository.observeArchived(),
        repository.observeCollections(),
    ) { active, archived, collections ->
        ManageUiState(active, archived, collections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageUiState())

    fun toggleFavorite(dhikr: Dhikr) {
        viewModelScope.launch { repository.setFavorite(dhikr.id, !dhikr.isFavorite) }
    }

    fun archive(dhikr: Dhikr) {
        viewModelScope.launch { repository.setArchived(dhikr.id, true) }
    }

    fun restore(dhikr: Dhikr) {
        viewModelScope.launch { repository.setArchived(dhikr.id, false) }
    }

    /**
     * Move-by-one rather than drag-and-drop: it works with a screen reader, needs no long-press,
     * and reordering a list of adhkar is rarely more than nudging one item.
     */
    fun move(dhikr: Dhikr, offset: Int) {
        val current = uiState.value.active
        val index = current.indexOfFirst { it.id == dhikr.id }
        val target = index + offset
        if (index < 0 || target !in current.indices) return
        val reordered = current.toMutableList().apply { add(target, removeAt(index)) }
        viewModelScope.launch { repository.reorder(reordered.map { it.id }) }
    }
}
