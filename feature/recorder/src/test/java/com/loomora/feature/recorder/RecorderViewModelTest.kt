package com.loomora.feature.recorder

import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeMarkerDao : MarkerDao {
    override suspend fun insertMarker(marker: MarkerEntity) {}
    override fun getMarkersForRecording(recordingId: String): Flow<List<MarkerEntity>> = MutableStateFlow(emptyList())
    override suspend fun deleteMarker(id: String) {}
}

class RecorderViewModelTest {

    @Test
    fun recorderUiState_defaultValuesAreValid() {
        val engine = AudioRecordEngine()
        val markerDao = FakeMarkerDao()
        val viewModel = RecorderViewModel(engine, markerDao)
        val state = viewModel.uiState.value
        assertEquals(0, state.markersCount)
        assertEquals(0f, state.amplitude, 0.001f)
    }
}
