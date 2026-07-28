package com.loomora.feature.recordingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.net.Uri
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.audio.player.AudioPlayerEngine
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.audio.waveform.WaveformAlgorithm
import com.loomora.core.audio.waveform.WaveformLoadState
import com.loomora.core.audio.waveform.WaveformRepository
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.DiarizationRevision
import com.loomora.core.model.InsightRevision
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.SpeakerAlias
import com.loomora.core.model.StorageUsageSummary
import com.loomora.core.model.TranscriptRevision
import com.loomora.core.model.repository.RecordingRepository
import com.loomora.core.database.entity.AnalysisJobEntity
import com.loomora.core.offlineai.AnalysisJobStatus
import com.loomora.core.offlineai.DiarizationRepository
import com.loomora.core.offlineai.InsightRepository
import com.loomora.core.offlineai.OfflineAnalysisStage
import com.loomora.core.offlineai.OfflineAnalysisCoordinator
import com.loomora.core.offlineai.OfflineProcessingQueue
import com.loomora.core.offlineai.TranscriptRepository
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
    val isLoading: Boolean = true,
    val storageUsage: StorageUsageSummary = StorageUsageSummary(),
    val suggestedExportFileName: String = "recording.m4a",
    val waveform: WaveformLoadState = WaveformLoadState.Idle,
    val transcript: TranscriptRevision? = null,
    val diarization: DiarizationRevision? = null,
    val insights: InsightRevision? = null,
    val speakerAliases: List<SpeakerAlias> = emptyList(),
    val exportProgress: Int? = null,
    val operationResult: RecordingOperationResult? = null,
    val shareIntent: Intent? = null
)

private data class InsightUiBundle(
    val transcript: TranscriptRevision?,
    val diarization: DiarizationRevision?,
    val aliases: List<SpeakerAlias>,
    val insights: InsightRevision?
)

@HiltViewModel
class RecordingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val markerDao: MarkerDao,
    private val recordingStorageManager: RecordingStorageManager,
    private val waveformRepository: WaveformRepository,
    val audioPlayerEngine: AudioPlayerEngine,
    val offlineAnalysisCoordinator: OfflineAnalysisCoordinator,
    private val offlineProcessingQueue: OfflineProcessingQueue,
    private val transcriptRepository: TranscriptRepository,
    private val diarizationRepository: DiarizationRepository,
    private val insightRepository: InsightRepository
) : ViewModel() {

    private val recordingId: String? = savedStateHandle["recordingId"]
    private val _recordingIdFlow = MutableStateFlow(recordingId)
    private val operationResult = MutableStateFlow<RecordingOperationResult?>(null)
    private val exportProgress = MutableStateFlow<Int?>(null)
    private val shareIntent = MutableStateFlow<Intent?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recordingFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) recordingRepository.getRecordingById(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val markersFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) markerDao.getMarkersForRecording(id) else flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val waveformFlow = recordingFlow.flatMapLatest { recording ->
        if (recording == null) {
            flowOf(WaveformLoadState.Idle)
        } else {
            waveformRepository.loadWaveform(recording, WaveformAlgorithm.DETAIL_RESOLUTION)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val transcriptFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) transcriptRepository.observeLatestTranscript(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val diarizationFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) diarizationRepository.observeLatestDiarization(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val speakerAliasesFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) diarizationRepository.observeAliases(id) else flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val insightsFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) insightRepository.observeLatestInsight(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roomAiJobStatusFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) {
            offlineProcessingQueue.observeJobsForRecording(id)
        } else {
            flowOf(emptyList())
        }
    }.combine(insightsFlow) { jobs, insights ->
        jobs.toAiJobStatus(insights)
    }

    val uiState: StateFlow<RecordingDetailUiState> = combine(
        combine(
            combine(
                recordingFlow,
                markersFlow,
                audioPlayerEngine.playerState,
                roomAiJobStatusFlow,
                operationResult
            ) { recording, markers, playerState, aiStatus, currentOperationResult ->
                RecordingDetailUiState(
                    recording = recording,
                    markers = markers,
                    playerState = playerState,
                    aiJobStatus = aiStatus,
                    isLoading = false,
                    storageUsage = recordingStorageManager.getStorageUsageSummary(),
                    suggestedExportFileName = recording?.let(recordingStorageManager::suggestedExportFileName)
                        ?: "recording.m4a",
                    operationResult = currentOperationResult
                )
            },
            combine(transcriptFlow, diarizationFlow, speakerAliasesFlow, insightsFlow) { currentTranscript, diarization, aliases, insights ->
                InsightUiBundle(currentTranscript, diarization, aliases, insights)
            }
        ) { baseState, transcriptState ->
            baseState.copy(
                transcript = transcriptState.transcript,
                diarization = transcriptState.diarization,
                speakerAliases = transcriptState.aliases,
                insights = transcriptState.insights
            )
        }.let { baseWithTranscript ->
            combine(
                baseWithTranscript,
            waveformFlow
            ) { baseState, waveform ->
            baseState.copy(
                waveform = waveform
            )
            }
        },
        exportProgress,
        shareIntent
    ) { baseState, currentExportProgress, currentShareIntent ->
        baseState.copy(
            exportProgress = currentExportProgress,
            shareIntent = currentShareIntent
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
        if (newTitle.isBlank()) return
        val id = recordingId ?: return
        viewModelScope.launch {
            operationResult.value = recordingRepository.renameRecording(id, newTitle)
        }
    }

    fun shareRecording() {
        val recording = uiState.value.recording ?: return
        shareIntent.value = null
        operationResult.value = recordingStorageManager.buildShareIntent(recording)
            .fold(
                onSuccess = {
                    shareIntent.value = it
                    RecordingOperationResult.Success
                },
                onFailure = { RecordingOperationResult.SourceMissing }
            )
    }

    fun consumeShareIntent() {
        shareIntent.value = null
    }

    fun exportRecording(destinationUri: Uri) {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            exportProgress.value = 0
            val result = recordingStorageManager.exportToDocument(recording, destinationUri) { progress ->
                exportProgress.value = progress
            }
            exportProgress.value = null
            operationResult.value = result
        }
    }

    fun onExportCancelled() {
        operationResult.value = RecordingOperationResult.ExportCancelled
    }

    fun softDeleteRecording() {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            waveformRepository.cancelGeneration(recording.id)
            operationResult.value = recordingRepository.softDeleteRecording(recording.id)
        }
    }

    fun restoreRecording() {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            operationResult.value = recordingRepository.restoreRecording(recording.id)
        }
    }

    fun permanentlyDeleteRecording() {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            waveformRepository.cancelGeneration(recording.id)
            operationResult.value = recordingRepository.deleteRecordingPermanently(recording.id)
        }
    }

    fun clearOperationResult() {
        operationResult.value = null
    }

    fun startAiProcessing(hasUserConsented: Boolean) {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            if (!hasUserConsented) {
                offlineAnalysisCoordinator.resetStatus()
                return@launch
            }
            offlineProcessingQueue.enqueue(recording.id, recording.originalFileUri)
        }
    }

    fun resetAiStatus() {
        offlineAnalysisCoordinator.resetStatus()
    }

    fun renameSpeaker(genericLabel: String, displayName: String) {
        val id = recordingId ?: return
        viewModelScope.launch {
            diarizationRepository.renameSpeaker(id, genericLabel, displayName)
        }
    }

    private fun List<AnalysisJobEntity>.toAiJobStatus(insightRevision: InsightRevision?): AiJobStatus {
        val job = maxByOrNull { it.updatedAt } ?: return AiJobStatus.Idle
        return when (job.status) {
            AnalysisJobStatus.QUEUED.name -> AiJobStatus.Queued(job.id)
            AnalysisJobStatus.RUNNING.name -> AiJobStatus.Processing(job.stage.toStageLabel(), job.progress)
            AnalysisJobStatus.CANCEL_REQUESTED.name -> AiJobStatus.Processing("Cancelling offline analysis", job.progress)
            AnalysisJobStatus.CANCELLED.name -> AiJobStatus.Cancelled
            AnalysisJobStatus.COMPLETED.name -> {
                val fallbackReason = job.fallbackReason
                if (fallbackReason != null && insightRevision != null) {
                    AiJobStatus.CompletedWithHeuristicFallback(
                        transcript = emptyList(),
                        insights = insightRevision.insights,
                        reason = fallbackReason
                    )
                } else {
                    AiJobStatus.Completed(emptyList(), insightRevision?.insights)
                }
            }
            AnalysisJobStatus.RETRYABLE_FAILURE.name,
            AnalysisJobStatus.TERMINAL_FAILURE.name -> AiJobStatus.Failed(
                message = job.errorCode ?: "Offline analysis failed.",
                isRetryable = job.status == AnalysisJobStatus.RETRYABLE_FAILURE.name
            )
            else -> AiJobStatus.Idle
        }
    }

    private fun String.toStageLabel(): String {
        return when (this) {
            OfflineAnalysisStage.PREPARING_AUDIO.name -> "Preparing local audio"
            OfflineAnalysisStage.TRANSCRIBING.name -> "Transcribing"
            OfflineAnalysisStage.DIARIZING.name -> "Diarizing speakers"
            OfflineAnalysisStage.ALIGNING.name -> "Aligning speakers"
            OfflineAnalysisStage.GENERATING_HEURISTIC_INSIGHTS.name -> "Generating heuristic insights"
            OfflineAnalysisStage.OPTIONAL_LLM_ENHANCEMENT.name -> "Optional LLM enhancement"
            OfflineAnalysisStage.VALIDATING.name -> "Validating local outputs"
            OfflineAnalysisStage.PUBLISHING.name -> "Publishing results"
            OfflineAnalysisStage.CLEANING_UP.name -> "Cleaning up"
            else -> "Offline analysis running"
        }
    }

    override fun onCleared() {
        audioPlayerEngine.release()
        super.onCleared()
    }
}
