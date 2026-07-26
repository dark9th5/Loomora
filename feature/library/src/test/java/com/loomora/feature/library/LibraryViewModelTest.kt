package com.loomora.feature.library

import com.loomora.core.model.Recording
import com.loomora.core.model.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeRecordingRepository : RecordingRepository {
    val recordingsFlow = MutableStateFlow<List<Recording>>(emptyList())

    override fun getActiveRecordings(): Flow<List<Recording>> = recordingsFlow
    override fun getFavoriteRecordings(): Flow<List<Recording>> = recordingsFlow
    override fun getTrashedRecordings(): Flow<List<Recording>> = recordingsFlow
    override fun getRecordingById(id: String): Flow<Recording?> = MutableStateFlow(null)
    override fun searchRecordings(query: String): Flow<List<Recording>> = recordingsFlow
    override suspend fun insertRecording(recording: Recording) {}
    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {}
    override suspend fun softDeleteRecording(id: String) {}
    override suspend fun restoreRecording(id: String) {}
    override suspend fun deleteRecordingPermanently(id: String) {}
}

class LibraryViewModelTest {

    @Test
    fun libraryUiState_defaultValuesAreValid() {
        val fakeRepo = FakeRecordingRepository()
        val viewModel = LibraryViewModel(fakeRepo)
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(SortOption.NEWEST, state.sortOption)
        assertEquals(false, state.showFavoritesOnly)
    }
}
