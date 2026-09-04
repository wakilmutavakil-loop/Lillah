package com.lillah.dhikr.ui.screens.editor

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CoverArt
import com.lillah.dhikr.domain.model.DhikrCollection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class CollectionEditorState(
    val id: Long = 0,
    val name: String = "",
    val arabicName: String = "",
    val description: String = "",
    val artwork: CoverArt = CoverArt.Bloom,
    val coverImagePath: String? = null,
    val accentIndex: Int = 1,
    val kind: CollectionKind = CollectionKind.Custom,
    val isBuiltIn: Boolean = false,
    val savingCover: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank()
    val title: String get() = if (isNew) "New collection" else "Edit collection"
}

class CollectionEditorViewModel(
    private val container: AppContainer,
    private val collectionId: Long,
) : ViewModel() {

    private val repository = container.dhikrRepository
    private val coverStore = container.coverImageStore

    private val _state = MutableStateFlow(CollectionEditorState())
    val state: StateFlow<CollectionEditorState> = _state.asStateFlow()

    init {
        if (collectionId != 0L) {
            viewModelScope.launch {
                repository.getCollection(collectionId)?.let { collection ->
                    _state.value = CollectionEditorState(
                        id = collection.id,
                        name = collection.name,
                        arabicName = collection.arabicName.orEmpty(),
                        description = collection.description.orEmpty(),
                        artwork = collection.artwork,
                        coverImagePath = collection.coverImagePath,
                        accentIndex = collection.accentIndex,
                        kind = collection.kind,
                        isBuiltIn = collection.isBuiltIn,
                    )
                }
            }
        }
    }

    fun update(transform: (CollectionEditorState) -> CollectionEditorState) {
        _state.value = transform(_state.value)
    }

    /**
     * A cover for a collection that has not been saved yet is written under id 0 and moved into
     * place on save; that keeps the file on disk from depending on a row that may never exist.
     */
    fun chooseCover(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(savingCover = true)
            val path = coverStore.save(_state.value.id, uri)
            if (path != null) {
                _state.value = _state.value.copy(coverImagePath = path)
            }
            _state.value = _state.value.copy(savingCover = false)
        }
    }

    /** Returns to the built-in artwork. The chosen image file is kept, not removed. */
    fun clearCover() {
        _state.value = _state.value.copy(coverImagePath = null)
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            val existing = if (current.isNew) null else repository.getCollection(current.id)
            val collection = (existing ?: DhikrCollection()).copy(
                id = current.id,
                name = current.name.trim(),
                arabicName = current.arabicName.trim().takeIf { it.isNotBlank() },
                description = current.description.trim().takeIf { it.isNotBlank() },
                artwork = current.artwork,
                coverImagePath = current.coverImagePath,
                accentIndex = current.accentIndex,
                kind = current.kind,
                isBuiltIn = current.isBuiltIn,
            )
            onSaved(repository.upsertCollection(collection))
        }
    }

    /**
     * Archiving hides a collection without touching it or the adhkar inside it. There is no
     * delete: everything counted under this collection stays counted, and the collection itself
     * can be brought back.
     */
    fun archive(onArchived: () -> Unit) {
        val current = _state.value
        if (current.id == 0L) return
        viewModelScope.launch {
            repository.getCollection(current.id)?.let {
                repository.upsertCollection(it)
            }
            repository.setCollectionArchived(current.id, true)
            onArchived()
        }
    }
}
