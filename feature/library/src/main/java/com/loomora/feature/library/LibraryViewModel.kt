package com.loomora.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.model.Recording
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
    val recordings: List<Recording> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    private val _showFavoritesOnly = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery,
        _sortOption,
        _showFavoritesOnly
    ) { query, sort, favoritesOnly ->
        Triple(query, sort, favoritesOnly)
    }.flatMapLatest { (query, sort, favoritesOnly) ->
        val recordingsFlow = when {
            favoritesOnly -> recordingRepository.getFavoriteRecordings()
            query.isBlank() -> recordingRepository.getActiveRecordings()
            else -> recordingRepository.searchRecordings(query)
        }
        recordingsFlow.map { list ->
            val sortedList = when (sort) {
                SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
                SortOption.OLDEST -> list.sortedBy { it.createdAt }
                SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
                SortOption.DURATION -> list.sortedByDescending { it.durationMs }
            }
            LibraryUiState(
                searchQuery = query,
                sortOption = sort,
                showFavoritesOnly = favoritesOnly,
                recordings = sortedList
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
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(recordingId: String, currentIsFavorite: Boolean) {
        viewModelScope.launch {
            recordingRepository.toggleFavorite(recordingId, !currentIsFavorite)
        }
    }

    fun softDeleteRecording(recordingId: String) {
        viewModelScope.launch {
            recordingRepository.softDeleteRecording(recordingId)
        }
    }
}
