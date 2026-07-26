package com.loomora.feature.recorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.audio.model.RecorderState
import com.loomora.core.audio.service.AudioRecorderService
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RecorderUiState(
    val state: RecorderState = RecorderState.Idle,
    val amplitude: Float = 0f,
    val markersCount: Int = 0
)

@HiltViewModel
class RecorderViewModel @Inject constructor(
    val audioRecordEngine: AudioRecordEngine,
    private val markerDao: MarkerDao
) : ViewModel() {

    private val _markersCount = kotlinx.coroutines.flow.MutableStateFlow(0)

    val uiState: StateFlow<RecorderUiState> = combine(
        audioRecordEngine.state,
        audioRecordEngine.amplitude,
        _markersCount
    ) { state, amplitude, markersCount ->
        RecorderUiState(
            state = state,
            amplitude = amplitude,
            markersCount = markersCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecorderUiState()
    )

    fun startRecording(context: Context, title: String = "New Recording") {
        AudioRecorderService.startService(context, title)
    }

    fun pauseRecording() {
        audioRecordEngine.pauseRecording()
    }

    fun resumeRecording() {
        audioRecordEngine.resumeRecording()
    }

    fun stopRecording() {
        audioRecordEngine.stopRecording()
    }

    fun addMarker() {
        val currentState = uiState.value.state
        val durationMs = when (currentState) {
            is RecorderState.Recording -> currentState.durationMs
            is RecorderState.Paused -> currentState.durationMs
            else -> 0L
        }

        viewModelScope.launch {
            markerDao.insertMarker(
                MarkerEntity(
                    id = UUID.randomUUID().toString(),
                    recordingId = "active",
                    timeMs = durationMs,
                    label = "Marker #${_markersCount.value + 1}",
                    createdAt = System.currentTimeMillis()
                )
            )
            _markersCount.value += 1
        }
    }
}
