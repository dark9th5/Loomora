package com.loomora.feature.recordingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.audio.player.AudioPlayerEngine
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.Recording
import com.loomora.core.model.repository.RecordingRepository
import com.loomora.core.network.AiPipelineEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordingDetailUiState(
    val recording: Recording? = null,
    val markers: List<MarkerEntity> = emptyList(),
    val playerState: PlayerState = PlayerState.Idle,
    val aiJobStatus: AiJobStatus = AiJobStatus.Idle,
    val isLoading: Boolean = true
)

@HiltViewModel
class RecordingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val markerDao: MarkerDao,
    val audioPlayerEngine: AudioPlayerEngine,
    val aiPipelineEngine: AiPipelineEngine
) : ViewModel() {

    private val recordingId: String? = savedStateHandle["recordingId"]
    private val _recordingIdFlow = MutableStateFlow(recordingId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recordingFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) recordingRepository.getRecordingById(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val markersFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) markerDao.getMarkersForRecording(id) else flowOf(emptyList())
    }

    val uiState: StateFlow<RecordingDetailUiState> = combine(
        recordingFlow,
        markersFlow,
        audioPlayerEngine.playerState,
        aiPipelineEngine.jobStatus
    ) { recording, markers, playerState, aiStatus ->
        RecordingDetailUiState(
            recording = recording,
            markers = markers,
            playerState = playerState,
            aiJobStatus = aiStatus,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecordingDetailUiState()
    )

    fun playAudio() {
        val recording = uiState.value.recording ?: return
        audioPlayerEngine.playAudio(recording.id, recording.originalFileUri)
    }

    fun pauseAudio() {
        audioPlayerEngine.pause()
    }

    fun resumeAudio() {
        audioPlayerEngine.resume()
    }

    fun seekTo(positionMs: Long) {
        audioPlayerEngine.seekTo(positionMs)
    }

    fun seekForward() {
        audioPlayerEngine.seekForward(10000L)
    }

    fun seekRewind() {
        audioPlayerEngine.seekRewind(10000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayerEngine.setPlaybackSpeed(speed)
    }

    fun toggleFavorite() {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            recordingRepository.toggleFavorite(recording.id, !recording.isFavorite)
        }
    }

    fun updateTitle(newTitle: String) {
        val recording = uiState.value.recording ?: return
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            recordingRepository.insertRecording(recording.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    fun startAiProcessing(hasUserConsented: Boolean) {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            aiPipelineEngine.processAudio(recording.originalFileUri, hasUserConsented)
        }
    }

    fun resetAiStatus() {
        aiPipelineEngine.resetStatus()
    }
}
