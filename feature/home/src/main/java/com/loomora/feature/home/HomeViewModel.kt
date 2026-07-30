package com.loomora.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.model.Recording
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val remainingTrialUses: Int = 3,
    val recentRecordings: List<Recording> = emptyList(),
    val activeAiRecording: Recording? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = recordingRepository.getActiveRecordings()
        .map { list ->
            HomeUiState(
                remainingTrialUses = 3,
                recentRecordings = list.take(5),
                activeAiRecording = list.firstOrNull {
                    it.transcriptStatus in setOf("QUEUED", "PROCESSING") ||
                        it.insightStatus in setOf("QUEUED", "PROCESSING")
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    fun toggleFavorite(recordingId: String, currentIsFavorite: Boolean) {
        viewModelScope.launch {
            recordingRepository.toggleFavorite(recordingId, !currentIsFavorite)
        }
    }
}
