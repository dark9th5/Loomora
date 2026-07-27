package com.loomora.feature.recorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.audio.model.RecorderState
import com.loomora.core.audio.service.AudioRecorderService
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RecorderUiState(
    val state: RecorderState = RecorderState.Idle,
    val amplitude: Float = 0f,
    val activeRecordingId: String? = null,
    val markersCount: Int = 0,
    val lowStorageWarning: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecorderViewModel @Inject constructor(
    val audioRecordEngine: AudioRecordEngine,
    private val markerDao: MarkerDao,
    private val recordingStorageManager: RecordingStorageManager
) : ViewModel() {

    private val startCommandInFlight = MutableStateFlow(false)
    private val lowStorageWarning = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            audioRecordEngine.state.collect { state ->
                if (state !is RecorderState.Idle && state !is RecorderState.Ready && state !is RecorderState.Saved) {
                    startCommandInFlight.value = false
                }
            }
        }
    }

    private val activeRecordingId = audioRecordEngine.state
        .map { state ->
            when (state) {
                is RecorderState.Recording -> state.recordingId
                is RecorderState.Paused -> state.recordingId
                is RecorderState.Saved -> state.recordingId
                else -> null
            }
        }
        .distinctUntilChanged()

    private val markersCount = activeRecordingId.flatMapLatest { recordingId ->
        if (recordingId == null) {
            flowOf(0)
        } else {
            markerDao.getMarkerCountForRecording(recordingId)
        }
    }

    val uiState: StateFlow<RecorderUiState> = combine(
        audioRecordEngine.state,
        audioRecordEngine.amplitude,
        activeRecordingId,
        markersCount,
        lowStorageWarning
    ) { state, amplitude, recordingId, markersCount, hasLowStorage ->
        RecorderUiState(
            state = state,
            amplitude = amplitude,
            activeRecordingId = recordingId,
            markersCount = markersCount,
            lowStorageWarning = hasLowStorage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecorderUiState()
    )

    fun startRecording(context: Context, title: String) {
        val currentState = uiState.value.state
        if (startCommandInFlight.value ||
            currentState is RecorderState.Preparing ||
            currentState is RecorderState.Recording ||
            currentState is RecorderState.Paused ||
            currentState is RecorderState.Finalizing ||
            currentState is RecorderState.Saving
        ) {
            return
        }
        if (!recordingStorageManager.hasAvailableBytes(MIN_RECORDING_START_BYTES)) {
            lowStorageWarning.value = true
            return
        }
        lowStorageWarning.value = false
        startCommandInFlight.value = true
        AudioRecorderService.startService(context, title)
    }

    fun pauseRecording(context: Context) {
        AudioRecorderService.pauseService(context, uiState.value.activeRecordingId)
    }

    fun resumeRecording(context: Context) {
        AudioRecorderService.resumeService(context, uiState.value.activeRecordingId)
    }

    fun stopRecording(context: Context) {
        AudioRecorderService.stopService(context, uiState.value.activeRecordingId)
    }

    fun addMarker(context: Context) {
        val currentState = uiState.value.state
        val recordingId: String
        val durationMs: Long
        when (currentState) {
            is RecorderState.Recording -> {
                recordingId = currentState.recordingId
                durationMs = currentState.durationMs
            }
            is RecorderState.Paused -> {
                recordingId = currentState.recordingId
                durationMs = currentState.durationMs
            }
            else -> return
        }

        viewModelScope.launch {
            val markerIndex = uiState.value.markersCount + 1
            markerDao.insertMarker(
                MarkerEntity(
                    id = UUID.randomUUID().toString(),
                    recordingId = recordingId,
                    timeMs = durationMs,
                    label = context.getString(com.loomora.core.designsystem.R.string.marker_default_label, markerIndex),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    companion object {
        private const val MIN_RECORDING_START_BYTES = 10L * 1024L * 1024L
    }
}
