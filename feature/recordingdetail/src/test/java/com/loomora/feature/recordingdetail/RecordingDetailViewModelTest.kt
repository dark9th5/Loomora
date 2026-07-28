package com.loomora.feature.recordingdetail

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.audio.player.AudioPlayerEngine
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.audio.waveform.AndroidAudioWaveformDecoder
import com.loomora.core.audio.waveform.WaveformCacheStore
import com.loomora.core.audio.waveform.WaveformRepository
import com.loomora.core.audio.waveform.WavAudioWaveformDecoder
import com.loomora.core.database.dao.MarkerDao
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.dao.DiarizationDao
import com.loomora.core.database.dao.InsightDao
import com.loomora.core.database.dao.TranscriptDao
import com.loomora.core.database.entity.DiarizationRevisionEntity
import com.loomora.core.database.entity.InsightChunkCheckpointEntity
import com.loomora.core.database.entity.InsightRevisionEntity
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.database.entity.SpeakerAliasEntity
import com.loomora.core.database.entity.SpeakerTurnEntity
import com.loomora.core.database.entity.TranscriptRevisionEntity
import com.loomora.core.database.entity.TranscriptSegmentEntity
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.StorageUsageSummary
import com.loomora.core.model.repository.RecordingRepository
import com.loomora.core.offlineai.AnalysisJobRepository
import com.loomora.core.offlineai.DefaultOfflineModelCatalog
import com.loomora.core.offlineai.ModelCompatibilityChecker
import com.loomora.core.offlineai.OfflineAnalysisCoordinator
import com.loomora.core.offlineai.OfflineModelImporter
import com.loomora.core.offlineai.OfflineModelRepository
import com.loomora.core.offlineai.AudioTranscriptionPreprocessor
import com.loomora.core.offlineai.DiarizationInput
import com.loomora.core.offlineai.DiarizationOutput
import com.loomora.core.offlineai.DiarizationRepository
import com.loomora.core.offlineai.InsightRepository
import com.loomora.core.offlineai.LocalDiarizationEngine
import com.loomora.core.offlineai.LocalMeetingInsightEngine
import com.loomora.core.offlineai.MeetingInsightInput
import com.loomora.core.offlineai.MeetingInsightOutput
import com.loomora.core.offlineai.OfflineEngineLifecycleManager
import com.loomora.core.offlineai.SherpaOnnxTranscriptionEngine
import com.loomora.core.offlineai.TranscriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.serialization.json.Json

private class FakeRecordingRepository : RecordingRepository {
    val recordingFlow = MutableStateFlow(
        Recording(
            id = "rec-1",
            title = "Test Note",
            createdAt = 1_000L,
            updatedAt = 1_000L,
            durationMs = 60_000L,
            status = RecordingStatus.SAVED,
            originalFileUri = "file:///tmp/test.aac"
        )
    )
    var renameResult: RecordingOperationResult = RecordingOperationResult.Success
    var softDeleteResult: RecordingOperationResult = RecordingOperationResult.Success
    var restoreResult: RecordingOperationResult = RecordingOperationResult.Success
    var permanentDeleteResult: RecordingOperationResult = RecordingOperationResult.Success
    var renamedTitle: String? = null

    override fun getActiveRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getRecoveryDiagnostics(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getFavoriteRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getTrashedRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getRecordingById(id: String): Flow<Recording?> = recordingFlow
    override fun searchRecordings(query: String): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override suspend fun insertRecording(recording: Recording) = Unit
    override suspend fun renameRecording(id: String, newTitle: String): RecordingOperationResult {
        renamedTitle = newTitle
        recordingFlow.value = recordingFlow.value.copy(title = newTitle)
        return renameResult
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) = Unit
    override suspend fun softDeleteRecording(id: String): RecordingOperationResult = softDeleteResult
    override suspend fun restoreRecording(id: String): RecordingOperationResult = restoreResult
    override suspend fun deleteRecordingPermanently(id: String): RecordingOperationResult = permanentDeleteResult
}

private class FakeMarkerDao : MarkerDao {
    override suspend fun insertMarker(marker: MarkerEntity) = Unit
    override fun getMarkersForRecording(recordingId: String): Flow<List<MarkerEntity>> = MutableStateFlow(emptyList())
    override fun getMarkerCountForRecording(recordingId: String): Flow<Int> = MutableStateFlow(0)
    override suspend fun deleteMarker(id: String) = Unit
}

private class FakeRecordingDao : RecordingDao {
    override suspend fun insertRecording(recording: RecordingEntity) = Unit
    override suspend fun updateRecording(recording: RecordingEntity) = Unit
    override fun getRecordingById(id: String): Flow<RecordingEntity?> = MutableStateFlow(null)
    override suspend fun getRecordingByIdSync(id: String): RecordingEntity? = null
    override fun getActiveRecordings(): Flow<List<RecordingEntity>> = MutableStateFlow(emptyList())
    override fun getRecoveryDiagnostics(): Flow<List<RecordingEntity>> = MutableStateFlow(emptyList())
    override suspend fun getCurrentRecordingSession(): RecordingEntity? = null
    override suspend fun getInterruptedRecordingSessions(): List<RecordingEntity> = emptyList()
    override suspend fun getRecordingByOriginalFileUriSync(fileUri: String): RecordingEntity? = null
    override fun getFavoriteRecordings(): Flow<List<RecordingEntity>> = MutableStateFlow(emptyList())
    override fun getTrashedRecordings(): Flow<List<RecordingEntity>> = MutableStateFlow(emptyList())
    override fun searchRecordings(query: String): Flow<List<RecordingEntity>> = MutableStateFlow(emptyList())
    override suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long) = Unit
    override suspend fun renameRecording(id: String, title: String, updatedAt: Long): Int = 1
    override suspend fun updateRecordingStatus(id: String, status: String, durationMs: Long, updatedAt: Long) = Unit
    override suspend fun updateTranscriptStatus(id: String, transcriptStatus: String, updatedAt: Long) = Unit
    override suspend fun updateInsightStatus(id: String, insightStatus: String, updatedAt: Long) = Unit
    override suspend fun updateRecoveredRecording(id: String, title: String, status: String, recoveryState: String, durationMs: Long, sizeBytes: Long, updatedAt: Long) = Unit
    override suspend fun updateRecoveryFailure(id: String, status: String, recoveryState: String, sizeBytes: Long, updatedAt: Long) = Unit
    override suspend fun softDeleteRecording(id: String, deletedAt: Long, updatedAt: Long): Int = 1
    override suspend fun restoreRecording(id: String, updatedAt: Long): Int = 1
    override suspend fun deleteRecordingPermanently(id: String): Int = 1
}

private class FakeTranscriptDao : TranscriptDao {
    override fun observeLatestRevision(recordingId: String): Flow<TranscriptRevisionEntity?> = MutableStateFlow(null)
    override fun observeSegmentsForRevision(revisionId: String): Flow<List<TranscriptSegmentEntity>> = MutableStateFlow(emptyList())
    override suspend fun getRevisionByIdentity(recordingId: String, sourceFingerprint: String, pipelineVersion: String, modelId: String, modelVersion: String): TranscriptRevisionEntity? = null
    override suspend fun getSegmentsForRevisionSync(revisionId: String): List<TranscriptSegmentEntity> = emptyList()
    override suspend fun upsertRevision(revision: TranscriptRevisionEntity) = Unit
    override suspend fun insertSegments(segments: List<TranscriptSegmentEntity>) = Unit
    override suspend fun deleteSegmentsForRevision(revisionId: String) = Unit
}

private class FakeDiarizationDao : DiarizationDao {
    override fun observeLatestRevision(recordingId: String): Flow<DiarizationRevisionEntity?> = MutableStateFlow(null)
    override fun observeTurnsForRevision(revisionId: String): Flow<List<SpeakerTurnEntity>> = MutableStateFlow(emptyList())
    override fun observeAliases(recordingId: String): Flow<List<SpeakerAliasEntity>> = MutableStateFlow(emptyList())
    override suspend fun getRevisionByIdentity(
        recordingId: String,
        sourceFingerprint: String,
        pipelineVersion: String,
        modelId: String,
        modelVersion: String,
        clusteringSettingsHash: String
    ): DiarizationRevisionEntity? = null
    override suspend fun getTurnsForRevisionSync(revisionId: String): List<SpeakerTurnEntity> = emptyList()
    override suspend fun upsertRevision(revision: DiarizationRevisionEntity) = Unit
    override suspend fun insertTurns(turns: List<SpeakerTurnEntity>) = Unit
    override suspend fun deleteTurnsForRevision(revisionId: String) = Unit
    override suspend fun upsertAlias(alias: SpeakerAliasEntity) = Unit
}

private class FakeDiarizationEngine : LocalDiarizationEngine {
    override suspend fun diarize(input: DiarizationInput): DiarizationOutput {
        return DiarizationOutput(
            turns = emptyList(),
            modelId = input.model.manifest.id,
            modelVersion = input.model.manifest.version,
            clusteringSettings = input.clustering,
            processingDurationMs = 0L,
            memoryObservationKb = null
        )
    }

    override fun close() = Unit
}

private class FakeInsightDao : InsightDao {
    override fun observeLatestRevision(recordingId: String): Flow<InsightRevisionEntity?> = MutableStateFlow(null)
    override fun observeLatestGeneratedRevision(recordingId: String): Flow<InsightRevisionEntity?> = MutableStateFlow(null)
    override fun observeLatestUserEditedRevision(recordingId: String): Flow<InsightRevisionEntity?> = MutableStateFlow(null)
    override suspend fun getRevisionByIdentity(recordingId: String, transcriptRevisionId: String, pipelineVersion: String, promptVersion: String, schemaVersion: String, modelId: String, modelVersion: String, kind: String): InsightRevisionEntity? = null
    override suspend fun getCheckpointsForRevision(revisionId: String): List<InsightChunkCheckpointEntity> = emptyList()
    override suspend fun upsertRevision(revision: InsightRevisionEntity) = Unit
    override suspend fun insertCheckpoints(checkpoints: List<InsightChunkCheckpointEntity>) = Unit
    override suspend fun deleteCheckpointsForRevision(revisionId: String) = Unit
}

private class FakeMeetingInsightEngine : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput {
        return MeetingInsightOutput(
            insights = com.loomora.core.model.AiInsights("Fixture", "Fixture summary"),
            modelId = input.model?.manifest?.id ?: com.loomora.core.offlineai.OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID,
            modelVersion = input.model?.manifest?.version ?: com.loomora.core.offlineai.OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_VERSION,
            promptVersion = com.loomora.core.offlineai.OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
            schemaVersion = com.loomora.core.offlineai.OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
            pipelineVersion = com.loomora.core.offlineai.OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
            languageTag = input.languageTag,
            chunkCheckpoints = emptyList(),
            modelSizeBytes = 1L,
            loadTimeMs = 1L,
            generationTimeMs = 1L,
            memoryObservationKb = 1L
        )
    }

    override fun close() = Unit
}

private class FakeRecordingStorageManager : RecordingStorageManager(
    ApplicationProvider.getApplicationContext()
) {
    var shareIntentResult: Result<Intent> = Result.success(Intent(Intent.ACTION_SEND))
    var exportResult: RecordingOperationResult = RecordingOperationResult.Success
    var exportProgressValues = mutableListOf<Int>()

    override fun getStorageUsageSummary(): StorageUsageSummary = StorageUsageSummary(freeBytes = 512L * 1024L * 1024L)

    override fun buildShareIntent(recording: Recording): Result<Intent> = shareIntentResult

    override suspend fun exportToDocument(
        recording: Recording,
        destinationUri: Uri,
        onProgress: (Int) -> Unit
    ): RecordingOperationResult {
        exportProgressValues.forEach(onProgress)
        return exportResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecordingDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultUiState_initialValuesAreValid() {
        val state = RecordingDetailUiState(
            recording = null,
            markers = emptyList(),
            playerState = PlayerState.Idle,
            isLoading = true
        )

        assertEquals(true, state.isLoading)
        assertEquals(PlayerState.Idle, state.playerState)
    }

    @Test
    fun updateTitle_persistsRenameResult() = runTest(dispatcher) {
        val repository = FakeRecordingRepository()
        val viewModel = createViewModel(repository = repository)
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTitle("Renamed Note")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Renamed Note", repository.renamedTitle)
        assertEquals("Renamed Note", repository.recordingFlow.value.title)
        collectionJob.cancel()
    }

    @Test
    fun shareRecording_reportsSuccessAndCanBeConsumed() = runTest(dispatcher) {
        val storageManager = FakeRecordingStorageManager().apply {
            shareIntentResult = Result.success(Intent(Intent.ACTION_SEND))
        }
        val viewModel = createViewModel(storageManager = storageManager)
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.shareRecording()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RecordingOperationResult.Success, viewModel.uiState.value.operationResult)

        viewModel.consumeShareIntent()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.shareIntent)
        collectionJob.cancel()
    }

    @Test
    fun exportRecording_surfacesStorageFailureWithoutRawException() = runTest(dispatcher) {
        val storageManager = FakeRecordingStorageManager().apply {
            exportResult = RecordingOperationResult.SourceMissing
            exportProgressValues = mutableListOf(0, 42)
        }
        val viewModel = createViewModel(storageManager = storageManager)
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.exportRecording(Uri.parse("content://loomora/export"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RecordingOperationResult.SourceMissing, viewModel.uiState.value.operationResult)
        assertNull(viewModel.uiState.value.exportProgress)
        assertEquals(listOf(0, 42), storageManager.exportProgressValues)
        collectionJob.cancel()
    }

    private fun createViewModel(
        repository: FakeRecordingRepository = FakeRecordingRepository(),
        storageManager: FakeRecordingStorageManager = FakeRecordingStorageManager()
    ): RecordingDetailViewModel {
        val analysisJobRepository = AnalysisJobRepository(
            analysisJobDao = object : com.loomora.core.database.dao.AnalysisJobDao {
                override suspend fun upsertJob(job: com.loomora.core.database.entity.AnalysisJobEntity) = Unit
                override fun observePendingJobs(): Flow<List<com.loomora.core.database.entity.AnalysisJobEntity>> = MutableStateFlow(emptyList())
                override fun observeJobsForRecording(recordingId: String): Flow<List<com.loomora.core.database.entity.AnalysisJobEntity>> = MutableStateFlow(emptyList())
                override suspend fun getJobByLogicalKey(logicalKey: String): com.loomora.core.database.entity.AnalysisJobEntity? = null
                override suspend fun getJobById(id: String): com.loomora.core.database.entity.AnalysisJobEntity? = null
                override suspend fun updateJobState(
                    id: String,
                    status: String,
                    stage: String,
                    progress: Float,
                    checkpointRef: String?,
                    stageOutputRef: String?,
                    modelVersionsJson: String,
                    errorCode: String?,
                    skipReason: String?,
                    fallbackReason: String?,
                    startedAt: Long?,
                    finishedAt: Long?,
                    updatedAt: Long
                ) = Unit
                override suspend fun updateWorkRequestId(id: String, workRequestId: String, updatedAt: Long) = Unit
                override suspend fun requestCancel(id: String, updatedAt: Long) = Unit
                override suspend fun reconcileRunningToQueued(updatedAt: Long) = Unit
            },
            json = Json { ignoreUnknownKeys = true }
        )
        return RecordingDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("recordingId" to "rec-1")),
            recordingRepository = repository,
            markerDao = FakeMarkerDao(),
            recordingStorageManager = storageManager,
            waveformRepository = WaveformRepository(
                cacheStore = WaveformCacheStore(ApplicationProvider.getApplicationContext()),
                wavDecoder = object : WavAudioWaveformDecoder() {
                    override fun canDecode(sourceFile: java.io.File): Boolean = false
                },
                androidDecoder = object : AndroidAudioWaveformDecoder() {
                    override fun canDecode(sourceFile: java.io.File): Boolean = false
                }
            ),
            audioPlayerEngine = AudioPlayerEngine(ApplicationProvider.getApplicationContext()),
            offlineAnalysisCoordinator = OfflineAnalysisCoordinator(
                modelRepository = OfflineModelRepository(
                    offlineModelDao = object : com.loomora.core.database.dao.OfflineModelDao {
                        override suspend fun upsertModel(model: com.loomora.core.database.entity.OfflineModelEntity) = Unit
                        override fun observeModels(): Flow<List<com.loomora.core.database.entity.OfflineModelEntity>> = MutableStateFlow(emptyList())
                        override suspend fun getAllModels(): List<com.loomora.core.database.entity.OfflineModelEntity> = emptyList()
                        override suspend fun getModelById(modelId: String): com.loomora.core.database.entity.OfflineModelEntity? = null
                        override suspend fun deleteModel(modelId: String) = Unit
                    },
                    importer = OfflineModelImporter(
                        context = ApplicationProvider.getApplicationContext(),
                        json = Json { ignoreUnknownKeys = true },
                        compatibilityChecker = ModelCompatibilityChecker(ApplicationProvider.getApplicationContext())
                    ),
                    compatibilityChecker = ModelCompatibilityChecker(ApplicationProvider.getApplicationContext()),
                    catalog = DefaultOfflineModelCatalog(),
                    json = Json { ignoreUnknownKeys = true }
                ),
                analysisJobRepository = analysisJobRepository,
                transcriptRepository = TranscriptRepository(FakeTranscriptDao()),
                diarizationRepository = DiarizationRepository(FakeDiarizationDao(), Json { ignoreUnknownKeys = true }),
                insightRepository = InsightRepository(FakeInsightDao(), Json { ignoreUnknownKeys = true }),
                recordingDao = FakeRecordingDao(),
                preprocessor = AudioTranscriptionPreprocessor(ApplicationProvider.getApplicationContext()),
                transcriptionEngine = SherpaOnnxTranscriptionEngine(ApplicationProvider.getApplicationContext()),
                diarizationEngine = FakeDiarizationEngine(),
                meetingInsightEngine = FakeMeetingInsightEngine(),
                engineLifecycleManager = OfflineEngineLifecycleManager()
            ),
            offlineProcessingQueue = com.loomora.core.offlineai.OfflineProcessingQueue(
                context = ApplicationProvider.getApplicationContext(),
                analysisJobRepository = analysisJobRepository
            ),
            transcriptRepository = TranscriptRepository(FakeTranscriptDao()),
            diarizationRepository = DiarizationRepository(FakeDiarizationDao(), Json { ignoreUnknownKeys = true }),
            insightRepository = InsightRepository(FakeInsightDao(), Json { ignoreUnknownKeys = true })
        )
    }
}
