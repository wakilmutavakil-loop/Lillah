package com.lillah.dhikr.ui.screens.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.DhikrCollection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class DhikrEditorState(
    val id: Long = 0,
    val name: String = "",
    val arabic: String = "",
    val transliteration: String = "",
    val meaning: String = "",
    val virtue: String = "",
    val source: String = "",
    val targetCount: Int = 33,
    val dailyTarget: Int? = null,
    val collectionId: Long? = null,
    val accentIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isBuiltIn: Boolean = false,
    val loaded: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank() || arabic.isNotBlank()
    val title: String get() = if (isNew) "New dhikr" else "Edit dhikr"
}

class DhikrEditorViewModel(
    private val container: AppContainer,
    private val dhikrId: Long,
    initialCollectionId: Long?,
) : ViewModel() {

    private val repository = container.dhikrRepository

    private val _state = MutableStateFlow(
        DhikrEditorState(collectionId = initialCollectionId, loaded = dhikrId == 0L)
    )
    val state: StateFlow<DhikrEditorState> = _state.asStateFlow()

    val collections: StateFlow<List<DhikrCollection>> = repository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (dhikrId != 0L) {
            viewModelScope.launch {
                repository.getDhikr(dhikrId)?.let { dhikr ->
                    _state.value = DhikrEditorState(
                        id = dhikr.id,
                        name = dhikr.name,
                        arabic = dhikr.arabic.orEmpty(),
                        transliteration = dhikr.transliteration.orEmpty(),
                        meaning = dhikr.meaning.orEmpty(),
                        virtue = dhikr.virtue.orEmpty(),
                        source = dhikr.source.orEmpty(),
                        targetCount = dhikr.targetCount,
                        dailyTarget = dhikr.dailyTarget,
                        collectionId = dhikr.collectionId,
                        accentIndex = dhikr.accentIndex,
                        isFavorite = dhikr.isFavorite,
                        isBuiltIn = dhikr.isBuiltIn,
                        loaded = true,
                    )
                }
            }
        }
    }

    fun update(transform: (DhikrEditorState) -> DhikrEditorState) {
        _state.value = transform(_state.value)
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            val existing = if (current.isNew) null else repository.getDhikr(current.id)
            val dhikr = (existing ?: Dhikr()).copy(
                id = current.id,
                name = current.name.trim().ifBlank { current.transliteration.trim() },
                arabic = current.arabic.trim().takeIf { it.isNotBlank() },
                transliteration = current.transliteration.trim().takeIf { it.isNotBlank() },
                meaning = current.meaning.trim().takeIf { it.isNotBlank() },
                virtue = current.virtue.trim().takeIf { it.isNotBlank() },
                source = current.source.trim().takeIf { it.isNotBlank() },
                targetCount = current.targetCount.coerceIn(1, 10_000),
                dailyTarget = current.dailyTarget?.coerceIn(1, 100_000),
                collectionId = current.collectionId,
                accentIndex = current.accentIndex,
                isFavorite = current.isFavorite,
                isBuiltIn = current.isBuiltIn,
            )
            val id = repository.upsert(dhikr)
            onSaved(id)
        }
    }

    /**
     * Deleting removes the dhikr's recorded history along with it, which is why the UI asks first
     * and offers archiving as the softer option.
     */
    fun delete(onDeleted: () -> Unit) {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.delete(id)
            onDeleted()
        }
    }

    fun archive(onArchived: () -> Unit) {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.setArchived(id, true)
            onArchived()
        }
    }
}
