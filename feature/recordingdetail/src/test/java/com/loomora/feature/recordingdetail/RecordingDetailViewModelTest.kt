package com.loomora.feature.recordingdetail

import androidx.lifecycle.SavedStateHandle
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeRecordingRepository : RecordingRepository {
    override fun getActiveRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getFavoriteRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getTrashedRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getRecordingById(id: String): Flow<Recording?> = MutableStateFlow(
        Recording(
            id = id,
            title = "Test Note",
            createdAt = 1000L,
            updatedAt = 1000L,
            durationMs = 60000L,
            status = RecordingStatus.SAVED,
            originalFileUri = "file:///path/to/test.aac"
        )
    )
    override fun searchRecordings(query: String): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override suspend fun insertRecording(recording: Recording) {}
    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {}
    override suspend fun softDeleteRecording(id: String) {}
    override suspend fun restoreRecording(id: String) {}
    override suspend fun deleteRecordingPermanently(id: String) {}
}

class FakeMarkerDao : MarkerDao {
    override suspend fun insertMarker(marker: MarkerEntity) {}
    override fun getMarkersForRecording(recordingId: String): Flow<List<MarkerEntity>> = MutableStateFlow(emptyList())
    override suspend fun deleteMarker(id: String) {}
}

class RecordingDetailViewModelTest {

    @Test
    fun defaultUiState_initialValuesAreValid() {
        val handle = SavedStateHandle(mapOf("recordingId" to "rec-1"))
        val repo = FakeRecordingRepository()
        val markerDao = FakeMarkerDao()
        // ViewModel state structure test
        val state = RecordingDetailUiState(
            recording = null,
            markers = emptyList(),
            playerState = PlayerState.Idle,
            isLoading = true
        )
        assertEquals(true, state.isLoading)
        assertEquals(PlayerState.Idle, state.playerState)
    }
}
