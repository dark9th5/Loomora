package com.loomora.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.model.Recording
import com.loomora.core.model.StorageUsageSummary
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption {
    NEWEST,
    OLDEST,
    TITLE,
    DURATION
}

data class LibraryUiState(
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NEWEST,
    val showFavoritesOnly: Boolean = false,
    val showTrashOnly: Boolean = false,
    val recordings: List<Recording> = emptyList(),
    val recoveryDiagnostics: List<Recording> = emptyList(),
    val trashedRecordings: List<Recording> = emptyList(),
    val storageUsage: StorageUsageSummary = StorageUsageSummary(),
    val lowStorageWarning: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordingStorageManager: RecordingStorageManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _showTrashOnly = MutableStateFlow(false)
    private val _dismissedRecoveryIds = MutableStateFlow<Set<String>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery,
        _sortOption,
        _showFavoritesOnly,
        _showTrashOnly,
        _dismissedRecoveryIds
    ) { query, sort, favoritesOnly, trashOnly, dismissedRecoveryIds ->
        LibraryFilters(
            query = query,
            sort = sort,
            favoritesOnly = favoritesOnly,
            trashOnly = trashOnly,
            dismissedRecoveryIds = dismissedRecoveryIds
        )
    }.flatMapLatest { filters ->
        val recordingsFlow = when {
            filters.trashOnly -> recordingRepository.getTrashedRecordings()
            filters.favoritesOnly -> recordingRepository.getFavoriteRecordings()
            filters.query.isBlank() -> recordingRepository.getActiveRecordings()
            else -> recordingRepository.searchRecordings(filters.query)
        }
        combine(
            recordingsFlow,
            recordingRepository.getRecoveryDiagnostics(),
            recordingRepository.getTrashedRecordings()
        ) { recordings, diagnostics, trashed ->
            val sortedRecordings = when (filters.sort) {
                SortOption.NEWEST -> recordings.sortedByDescending { it.createdAt }
                SortOption.OLDEST -> recordings.sortedBy { it.createdAt }
                SortOption.TITLE -> recordings.sortedBy { it.title.lowercase() }
                SortOption.DURATION -> recordings.sortedByDescending { it.durationMs }
            }

            val filteredDiagnostics = diagnostics
                .filter { it.id !in filters.dismissedRecoveryIds }
                .filter { diagnostic ->
                    !filters.trashOnly && (
                        filters.query.isBlank() ||
                        diagnostic.title.contains(filters.query, ignoreCase = true)
                    )
                }
                .sortedByDescending { it.createdAt }

            val filteredTrash = trashed
                .filter { trashedRecording ->
                    filters.query.isBlank() ||
                        trashedRecording.title.contains(filters.query, ignoreCase = true)
                }
                .sortedByDescending { it.updatedAt }

            LibraryUiState(
                searchQuery = filters.query,
                sortOption = filters.sort,
                showFavoritesOnly = filters.favoritesOnly,
                showTrashOnly = filters.trashOnly,
                recordings = sortedRecordings,
                recoveryDiagnostics = filteredDiagnostics,
                trashedRecordings = filteredTrash,
                storageUsage = recordingStorageManager.getStorageUsageSummary(),
                lowStorageWarning = recordingStorageManager.availableBytes() < LOW_STORAGE_WARNING_BYTES
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChange(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun toggleFavoritesOnly() {
        _showTrashOnly.value = false
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleTrashOnly() {
        if (!_showTrashOnly.value) {
            _showFavoritesOnly.value = false
        }
        _showTrashOnly.value = !_showTrashOnly.value
    }

    fun toggleFavorite(recordingId: String, currentIsFavorite: Boolean) {
        viewModelScope.launch {
            recordingRepository.toggleFavorite(recordingId, !currentIsFavorite)
        }
    }

    fun keepRecoveryDiagnostic(recordingId: String) {
        _dismissedRecoveryIds.value = _dismissedRecoveryIds.value + recordingId
    }

    fun deleteRecoveryDiagnostic(recordingId: String) {
        _dismissedRecoveryIds.value = _dismissedRecoveryIds.value + recordingId
        viewModelScope.launch {
            recordingRepository.deleteRecordingPermanently(recordingId)
        }
    }

    fun softDeleteRecording(recordingId: String) {
        viewModelScope.launch {
            recordingRepository.softDeleteRecording(recordingId)
        }
    }

    fun restoreRecording(recordingId: String) {
        viewModelScope.launch {
            recordingRepository.restoreRecording(recordingId)
        }
    }

    fun permanentlyDeleteRecording(recordingId: String) {
        viewModelScope.launch {
            recordingRepository.deleteRecordingPermanently(recordingId)
        }
    }

    companion object {
        private const val LOW_STORAGE_WARNING_BYTES = 256L * 1024L * 1024L
    }
}

private data class LibraryFilters(
    val query: String,
    val sort: SortOption,
    val favoritesOnly: Boolean,
    val trashOnly: Boolean,
    val dismissedRecoveryIds: Set<String>
)
