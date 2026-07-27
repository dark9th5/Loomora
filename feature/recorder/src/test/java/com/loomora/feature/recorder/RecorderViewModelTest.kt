package com.loomora.feature.recorder

import androidx.test.core.app.ApplicationProvider
import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.audio.model.RecorderState
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class RecorderViewModelTest {

    @Test
    fun recorderUiState_defaultValuesAreValid() {
        val engine = AudioRecordEngine()
        val markerDao = object : MarkerDao {
            override suspend fun insertMarker(marker: MarkerEntity) {}
            override fun getMarkersForRecording(recordingId: String): Flow<List<MarkerEntity>> = MutableStateFlow(emptyList())
            override fun getMarkerCountForRecording(recordingId: String): Flow<Int> = MutableStateFlow(0)
            override suspend fun deleteMarker(id: String) {}
        }
        val storageManager = object : RecordingStorageManager(ApplicationProvider.getApplicationContext()) {
            override fun hasAvailableBytes(requiredBytes: Long): Boolean = true
        }
        val viewModel = RecorderViewModel(engine, markerDao, storageManager)
        val state = viewModel.uiState.value
        assertEquals(0, state.markersCount)
        assertEquals(0f, state.amplitude, 0.001f)
    }

    @Test
    fun lowStorageStartAttempt_keepsRecorderIdle() = runTest {
        val engine = AudioRecordEngine()
        val markerDao = object : MarkerDao {
            override suspend fun insertMarker(marker: MarkerEntity) {}
            override fun getMarkersForRecording(recordingId: String): Flow<List<MarkerEntity>> = MutableStateFlow(emptyList())
            override fun getMarkerCountForRecording(recordingId: String): Flow<Int> = MutableStateFlow(0)
            override suspend fun deleteMarker(id: String) {}
        }
        val storageManager = object : RecordingStorageManager(ApplicationProvider.getApplicationContext()) {
            override fun hasAvailableBytes(requiredBytes: Long): Boolean = false
        }
        val viewModel = RecorderViewModel(engine, markerDao, storageManager)
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.startRecording(ApplicationProvider.getApplicationContext(), "Test")
        testScheduler.advanceUntilIdle()

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        collectionJob.cancel()
    }
}
