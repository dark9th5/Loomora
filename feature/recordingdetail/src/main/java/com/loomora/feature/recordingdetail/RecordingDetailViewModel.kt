package com.loomora.feature.recordingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.content.Context
import android.net.Uri
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.audio.player.AudioPlayerEngine
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.audio.waveform.WaveformAlgorithm
import com.loomora.core.audio.waveform.WaveformLoadState
import com.loomora.core.audio.waveform.WaveformRepository
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.dao.RecordingTaskDao
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.database.entity.RecordingTaskEntity
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.AiProcessingStage
import com.loomora.core.model.DiarizationRevision
import com.loomora.core.model.InsightRevision
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.SpeakerAlias
import com.loomora.core.model.StorageUsageSummary
import com.loomora.core.model.TranscriptRevision
import com.loomora.core.model.repository.RecordingRepository
import com.loomora.core.database.entity.AnalysisJobEntity
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import com.loomora.core.datastore.DefaultAnalysisMode
import com.loomora.core.datastore.OfflinePerformanceMode
import com.loomora.core.offlineai.AnalysisJobStatus
import com.loomora.core.offlineai.DiarizationRepository
import com.loomora.core.offlineai.InsightRepository
import com.loomora.core.offlineai.OfflineAnalysisStage
import com.loomora.core.offlineai.OfflineAnalysisCoordinator
import com.loomora.core.offlineai.OfflineProcessingQueue
import com.loomora.core.offlineai.OfflineProcessingOptions
import com.loomora.core.offlineai.TranscriptionPerformanceProfile
import com.loomora.core.offlineai.TranscriptionModelSelector
import com.loomora.core.offlineai.TranscriptRepository
import com.loomora.core.offlineai.TranscriptSpeakerFusion
import com.loomora.core.offlineai.ModelCapability
import com.loomora.core.offlineai.OfflineModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RecordingDetailUiState(
    val recording: Recording? = null,
    val markers: List<MarkerEntity> = emptyList(),
    val tasks: List<RecordingTaskEntity> = emptyList(),
    val playerState: PlayerState = PlayerState.Idle,
    val aiJobStatus: AiJobStatus = AiJobStatus.Idle,
    val isLoading: Boolean = true,
    val storageUsage: StorageUsageSummary = StorageUsageSummary(),
    val suggestedExportFileName: String = "recording.m4a",
    val suggestedInsightsFileName: String = "recording-analysis.txt",
    val waveform: WaveformLoadState = WaveformLoadState.Idle,
    val transcript: TranscriptRevision? = null,
    val diarization: DiarizationRevision? = null,
    val insights: InsightRevision? = null,
    val speakerAliases: List<SpeakerAlias> = emptyList(),
    val exportProgress: Int? = null,
    val operationResult: RecordingOperationResult? = null,
    val shareIntent: Intent? = null,
    val activeAnalysisJob: AnalysisJobEntity? = null,
    val isTranscriptionModelMissing: Boolean = false,
    val isDiarizationModelMissing: Boolean = false
)

private data class InsightUiBundle(
    val transcript: TranscriptRevision?,
    val diarization: DiarizationRevision?,
    val aliases: List<SpeakerAlias>,
    val insights: InsightRevision?
)

private data class AnalysisJobUiBundle(
    val status: AiJobStatus,
    val activeJob: AnalysisJobEntity?
)

@HiltViewModel
class RecordingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val markerDao: MarkerDao,
    private val recordingTaskDao: RecordingTaskDao,
    private val recordingStorageManager: RecordingStorageManager,
    private val waveformRepository: WaveformRepository,
    val audioPlayerEngine: AudioPlayerEngine,
    val offlineAnalysisCoordinator: OfflineAnalysisCoordinator,
    private val offlineProcessingQueue: OfflineProcessingQueue,
    private val transcriptRepository: TranscriptRepository,
    private val diarizationRepository: DiarizationRepository,
    private val insightRepository: InsightRepository,
    private val preferencesDataSource: LoomoraPreferencesDataSource,
    private val offlineModelRepository: OfflineModelRepository
) : ViewModel() {

    private val recordingId: String? = savedStateHandle["recordingId"]
    private val _recordingIdFlow = MutableStateFlow(recordingId)
    private val operationResult = MutableStateFlow<RecordingOperationResult?>(null)
    private val exportProgress = MutableStateFlow<Int?>(null)
    private val shareIntent = MutableStateFlow<Intent?>(null)
    private val transcriptionModelMissing = MutableStateFlow(false)
    private val diarizationModelMissing = MutableStateFlow(false)

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
    private val tasksFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) recordingTaskDao.observeTasksForRecording(id) else flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roomAiJobStatusFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) {
            offlineProcessingQueue.observeJobsForRecording(id)
        } else {
            flowOf(emptyList())
        }
    }.combine(insightsFlow) { jobs, insights ->
        AnalysisJobUiBundle(
            status = jobs.toAiJobStatus(insights),
            activeJob = jobs.maxByOrNull { it.updatedAt }?.takeIf {
                it.status in setOf(
                    AnalysisJobStatus.QUEUED.name,
                    AnalysisJobStatus.RUNNING.name,
                    AnalysisJobStatus.CANCEL_REQUESTED.name
                )
            }
        )
    }

    val uiState: StateFlow<RecordingDetailUiState> = combine(
        combine(
            combine(
                recordingFlow,
                markersFlow,
                audioPlayerEngine.playerState,
                roomAiJobStatusFlow,
                operationResult
            ) { recording, markers, playerState, aiBundle, currentOperationResult ->
                RecordingDetailUiState(
                    recording = recording,
                    markers = markers,
                    playerState = playerState,
                    aiJobStatus = aiBundle.status,
                    activeAnalysisJob = aiBundle.activeJob,
                    isLoading = false,
                    storageUsage = recordingStorageManager.getStorageUsageSummary(),
                    suggestedExportFileName = recording?.let(recordingStorageManager::suggestedExportFileName)
                        ?: "recording.m4a",
                    suggestedInsightsFileName = recording?.title
                        ?.replace(Regex("[^A-Za-z0-9._-]+"), "-")
                        ?.trim('-')
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "$it-analysis.txt" }
                        ?: "recording-analysis.txt",
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
        shareIntent,
        transcriptionModelMissing,
        diarizationModelMissing
    ) { baseState, currentExportProgress, currentShareIntent, modelMissing, speakerModelMissing ->
        baseState.copy(
            exportProgress = currentExportProgress,
            shareIntent = currentShareIntent,
            isTranscriptionModelMissing = modelMissing,
            isDiarizationModelMissing = speakerModelMissing
        )
    }.let { baseStateFlow ->
        combine(baseStateFlow, tasksFlow) { state, tasks ->
            state.copy(tasks = tasks)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecordingDetailUiState()
    )

    init {
        viewModelScope.launch {
            insightsFlow.filterNotNull().collectLatest { revision ->
                syncGeneratedTasks(revision)
            }
        }
    }

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

    fun playFrom(positionMs: Long) {
        val recording = uiState.value.recording ?: return
        audioPlayerEngine.playAudio(recording.id, recording.originalFileUri, positionMs)
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

    fun shareInsights() {
        val state = uiState.value
        val revision = state.insights ?: return
        shareIntent.value = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, revision.insights.suggestedTitle)
            putExtra(Intent.EXTRA_TEXT, formatInsightsText(state))
        }
    }

    fun exportInsights(destinationUri: Uri) {
        val state = uiState.value
        if (state.insights == null) return
        viewModelScope.launch {
            operationResult.value = runCatching {
                context.contentResolver.openOutputStream(destinationUri, "wt")?.bufferedWriter()?.use {
                    it.write(formatInsightsText(state))
                } ?: error("Unable to open destination")
            }.fold(
                onSuccess = { RecordingOperationResult.Success },
                onFailure = { RecordingOperationResult.FileSystemFailure(it.message.orEmpty()) }
            )
        }
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

    fun startAiProcessing(hasUserConsented: Boolean, analysisModeOverride: DefaultAnalysisMode? = null) {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            if (!hasUserConsented) {
                offlineAnalysisCoordinator.resetStatus()
                return@launch
            }
            val preferences = preferencesDataSource.userPreferences.first()
            val analysisMode = analysisModeOverride ?: preferences.defaultAnalysisMode
            val selectedTranscriptionModel = TranscriptionModelSelector.select(
                requestedLanguageTag = preferences.transcriptLanguage.tag,
                records = offlineModelRepository.models.first(),
                manuallySelectedId = preferences.transcriptionModelId
            )
            if (selectedTranscriptionModel == null) {
                transcriptionModelMissing.value = true
                return@launch
            }
            if (
                analysisMode == DefaultAnalysisMode.FULL_ANALYSIS &&
                !offlineModelRepository.hasReadyModels(setOf(ModelCapability.DIARIZATION))
            ) {
                diarizationModelMissing.value = true
                return@launch
            }
            offlineProcessingQueue.enqueue(
                recording.id,
                recording.editedOutputUri ?: recording.originalFileUri,
                OfflineProcessingOptions(
                    transcriptionModelId = selectedTranscriptionModel.manifest.id,
                    diarizationEnabled = analysisMode == DefaultAnalysisMode.FULL_ANALYSIS,
                    insightsMode = if (analysisMode == DefaultAnalysisMode.QUICK_TRANSCRIPT) "NONE" else "HEURISTIC",
                    outputLanguage = preferences.transcriptLanguage.tag,
                    optionalLlmEnhancement = analysisMode == DefaultAnalysisMode.FULL_ANALYSIS,
                    forceReanalysis = uiState.value.transcript != null,
                    performanceProfile = when (preferences.offlinePerformanceMode) {
                        OfflinePerformanceMode.BATTERY_SAVER -> TranscriptionPerformanceProfile.BATTERY_SAVER
                        OfflinePerformanceMode.BALANCED -> TranscriptionPerformanceProfile.BALANCED
                        OfflinePerformanceMode.FAST -> TranscriptionPerformanceProfile.FAST
                    }
                )
            )
        }
    }

    fun dismissMissingModelMessage() {
        transcriptionModelMissing.value = false
        diarizationModelMissing.value = false
    }

    fun resetAiStatus() {
        offlineAnalysisCoordinator.resetStatus()
    }

    fun cancelAiProcessing() {
        val job = uiState.value.activeAnalysisJob ?: return
        viewModelScope.launch { offlineProcessingQueue.cancel(job.id) }
    }

    fun renameSpeaker(genericLabel: String, displayName: String) {
        val id = recordingId ?: return
        viewModelScope.launch {
            diarizationRepository.renameSpeaker(id, genericLabel, displayName)
        }
    }

    fun updateInsights(title: String, summary: String) {
        val base = uiState.value.insights ?: return
        if (title.isBlank() || summary.isBlank()) return
        viewModelScope.launch {
            insightRepository.publishUserEditedRevision(
                base = base,
                editedInsights = base.insights.copy(
                    suggestedTitle = title.trim(),
                    summary = summary.trim()
                )
            )
        }
    }

    fun setTaskCompleted(taskId: String, completed: Boolean) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            recordingTaskDao.updateStatus(
                taskId = taskId,
                status = if (completed) RecordingTaskEntity.STATUS_DONE else RecordingTaskEntity.STATUS_TODO,
                completedAt = now.takeIf { completed },
                updatedAt = now
            )
        }
    }

    fun updateTask(taskId: String, title: String, assignee: String?, dueDate: String?) {
        val cleanTitle = title.replace(Regex("\\s+"), " ").trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            recordingTaskDao.updateContent(
                taskId = taskId,
                title = cleanTitle,
                assignee = assignee?.trim()?.takeIf(String::isNotBlank),
                dueDate = dueDate?.trim()?.takeIf(String::isNotBlank),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun archiveTask(taskId: String) {
        viewModelScope.launch {
            recordingTaskDao.archive(taskId, System.currentTimeMillis())
        }
    }

    private suspend fun syncGeneratedTasks(revision: InsightRevision) {
        val targetRecordingId = recordingId ?: return
        val now = System.currentTimeMillis()
        val generated = revision.insights.actionItems.mapIndexedNotNull { index, item ->
            val title = item.task.replace(Regex("\\s+"), " ").trim()
            if (title.isBlank()) return@mapIndexedNotNull null
            val assignee = item.assignee?.trim()?.takeIf(String::isNotBlank)
            val dueDate = item.dueDate?.trim()?.takeIf(String::isNotBlank)

            val sortedEvidenceCsv = item.evidenceSegmentIds
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .joinToString(",")

            val identityString = listOf(
                targetRecordingId,
                sortedEvidenceCsv,
                normalizeTaskIdentity(title),
                normalizeTaskIdentity(assignee.orEmpty())
            ).joinToString("|")

            val taskId = UUID.nameUUIDFromBytes(identityString.toByteArray(Charsets.UTF_8)).toString()

            RecordingTaskEntity(
                id = taskId,
                recordingId = targetRecordingId,
                sourceInsightRevisionId = revision.id,
                sourceActionIndex = index,
                title = title,
                assignee = assignee,
                dueDate = dueDate,
                evidenceSegmentIdsCsv = sortedEvidenceCsv,
                sourceGenerationMode = revision.generationMode,
                createdAt = now + index,
                updatedAt = now
            )
        }.distinctBy(RecordingTaskEntity::id)
        if (generated.isNotEmpty()) {
            recordingTaskDao.insertGeneratedTasks(generated)
            generated.forEach { task ->
                recordingTaskDao.updateSourceMetadata(
                    taskId = task.id,
                    sourceInsightRevisionId = task.sourceInsightRevisionId,
                    sourceActionIndex = task.sourceActionIndex,
                    sourceGenerationMode = task.sourceGenerationMode,
                    updatedAt = now
                )
            }
        }
    }

    private fun normalizeTaskIdentity(value: String): String = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    private fun List<AnalysisJobEntity>.toAiJobStatus(insightRevision: InsightRevision?): AiJobStatus {
        val job = maxByOrNull { it.updatedAt } ?: return AiJobStatus.Idle
        return when (job.status) {
            AnalysisJobStatus.QUEUED.name -> AiJobStatus.Queued(job.id)
            AnalysisJobStatus.RUNNING.name -> AiJobStatus.Processing(job.stage.toProcessingStage(), job.progress)
            AnalysisJobStatus.CANCEL_REQUESTED.name -> AiJobStatus.Processing(AiProcessingStage.CANCELLING, job.progress)
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
                message = "",
                isRetryable = job.status == AnalysisJobStatus.RETRYABLE_FAILURE.name,
                stage = job.stage.toProcessingStage()
            )
            else -> AiJobStatus.Idle
        }
    }

    private fun String.toProcessingStage(): AiProcessingStage {
        return when (this) {
            OfflineAnalysisStage.PREPARING_AUDIO.name -> AiProcessingStage.PREPARING_AUDIO
            OfflineAnalysisStage.TRANSCRIBING.name -> AiProcessingStage.TRANSCRIBING
            OfflineAnalysisStage.DIARIZING.name -> AiProcessingStage.DIARIZING
            OfflineAnalysisStage.ALIGNING.name -> AiProcessingStage.ALIGNING
            OfflineAnalysisStage.GENERATING_HEURISTIC_INSIGHTS.name -> AiProcessingStage.GENERATING_INSIGHTS
            OfflineAnalysisStage.OPTIONAL_LLM_ENHANCEMENT.name -> AiProcessingStage.OPTIONAL_ENHANCEMENT
            OfflineAnalysisStage.VALIDATING.name -> AiProcessingStage.VALIDATING
            OfflineAnalysisStage.PUBLISHING.name -> AiProcessingStage.PUBLISHING
            OfflineAnalysisStage.CLEANING_UP.name -> AiProcessingStage.CLEANING_UP
            else -> AiProcessingStage.RUNNING
        }
    }

    override fun onCleared() {
        audioPlayerEngine.release()
        super.onCleared()
    }
}

internal fun formatInsightsText(state: RecordingDetailUiState): String {
    val revision = state.insights ?: return ""
    val insights = revision.insights
    val vietnamese = revision.languageTag?.startsWith("vi", ignoreCase = true) == true
    val transcript = state.transcript
    val aliases = state.speakerAliases.associate { it.genericLabel to it.displayName }
    val headingSummary = if (vietnamese) "TOM TAT" else "SUMMARY"
    val headingNotes = if (vietnamese) "GHI CHU CHINH" else "KEY NOTES"
    val headingDecisions = if (vietnamese) "QUYET DINH" else "DECISIONS"
    val headingTasks = if (vietnamese) "VIEC CAN LAM" else "ACTION ITEMS"
    val headingQuestions = if (vietnamese) "CAU HOI MO" else "OPEN QUESTIONS"
    val headingTimeline = if (vietnamese) "DONG THOI GIAN NGUOI NOI" else "SPEAKER TIMELINE"

    return buildString {
        appendLine(insights.suggestedTitle)
        appendLine()
        appendLine(headingSummary)
        appendLine(insights.summary)

        fun appendList(title: String, values: List<String>) {
            if (values.isEmpty()) return
            appendLine()
            appendLine(title)
            values.forEach { appendLine("- $it") }
        }
        appendList(headingNotes, insights.keyPoints)
        appendList(headingDecisions, insights.decisions)
        val taskLines = if (state.tasks.isNotEmpty()) {
            state.tasks.map { task ->
                buildString {
                    append(if (task.isCompleted) "[x] " else "[ ] ")
                    append(task.title)
                    task.assignee?.let { append(" - $it") }
                    task.dueDate?.let { append(" - $it") }
                }
            }
        } else {
            insights.actionItems.map { item ->
                listOfNotNull(item.task, item.assignee, item.dueDate).joinToString(" - ")
            }
        }
        appendList(headingTasks, taskLines)
        appendList(headingQuestions, insights.openQuestions)

        val speakerRows = TranscriptSpeakerFusion.displayRows(transcript?.segments.orEmpty())
        if (speakerRows.isNotEmpty()) {
            appendLine()
            appendLine(headingTimeline)
            speakerRows.forEach { segment ->
                val label = segment.speakerLabel?.let { aliases[it] ?: it }
                    ?: if (vietnamese) "Người nói chưa xác định" else "Unknown speaker"
                append("${formatExportDuration(segment.startMs)}-${formatExportDuration(segment.endMs)} $label")
                if (segment.text.isNotBlank()) append(": ${segment.text.trim()}")
                appendLine()
            }
        }
    }.trim()
}

private fun formatExportDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
