package com.lillah.dhikr.ui.screens.collections

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.domain.model.Dhikr
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CollectionsUiState(
    val isLoading: Boolean = true,
    val featured: List<CollectionProgress> = emptyList(),
    val others: List<CollectionProgress> = emptyList(),
    val favorites: List<Dhikr> = emptyList(),
    val totalAdhkar: Int = 0,
)

class CollectionsViewModel(private val container: AppContainer) : ViewModel() {

    private val dhikrRepository = container.dhikrRepository
    private val settingsRepository = container.settingsRepository

    val uiState: StateFlow<CollectionsUiState> = combine(
        dhikrRepository.observeCollectionProgress(),
        dhikrRepository.observeFavorites(),
        dhikrRepository.observeAll(),
    ) { collections, favorites, all ->
        // Morning and Evening lead the screen; everything else follows in the user's own order.
        val featuredKinds = listOf(CollectionKind.Morning, CollectionKind.Evening)
        CollectionsUiState(
            isLoading = false,
            featured = featuredKinds.mapNotNull { kind ->
                collections.firstOrNull { it.collection.kind == kind }
            },
            others = collections.filterNot { it.collection.kind in featuredKinds },
            favorites = favorites,
            totalAdhkar = all.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState())

    fun selectDhikr(id: Long, onSelected: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setActiveDhikr(id)
            onSelected()
        }
    }
}
