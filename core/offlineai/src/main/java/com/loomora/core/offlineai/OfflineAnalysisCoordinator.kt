package com.loomora.core.offlineai

import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.model.AiJobStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineAnalysisCoordinator @Inject constructor(
    private val modelRepository: OfflineModelRepository,
    private val analysisJobRepository: AnalysisJobRepository,
    private val transcriptRepository: TranscriptRepository,
    private val diarizationRepository: DiarizationRepository,
    private val insightRepository: InsightRepository,
    private val recordingDao: RecordingDao,
    private val preprocessor: AudioTranscriptionPreprocessor,
    private val transcriptionEngine: LocalTranscriptionEngine,
    private val diarizationEngine: LocalDiarizationEngine,
    private val meetingInsightEngine: LocalMeetingInsightEngine,
    private val engineLifecycleManager: OfflineEngineLifecycleManager
) {
    private val _jobStatus = MutableStateFlow<AiJobStatus>(AiJobStatus.Idle)
    val jobStatus: StateFlow<AiJobStatus> = _jobStatus.asStateFlow()
    private var activeJob: Job? = null

    suspend fun processAudio(recordingId: String, audioFileUri: String) {
        processAudioInternal(recordingId, audioFileUri, existingJobId = null)
    }

    suspend fun processAudioForJob(jobId: String, recordingId: String, audioFileUri: String) {
        processAudioInternal(recordingId, audioFileUri, existingJobId = jobId)
    }

    private suspend fun processAudioInternal(
        recordingId: String,
        audioFileUri: String,
        existingJobId: String?
    ) {
        activeJob = currentCoroutineContext()[Job]
        val sourceFile = File(audioFileUri.removePrefix("file://"))
        if (!sourceFile.exists() || !sourceFile.isFile) {
            _jobStatus.value = AiJobStatus.Failed(
                message = userMessage(OfflineAiException.FileMissing),
                isRetryable = false
            )
            recordingDao.updateTranscriptStatus(recordingId, "FAILED", System.currentTimeMillis())
            return
        }

        _jobStatus.value = AiJobStatus.VerifyingModels
        recordingDao.updateTranscriptStatus(recordingId, "VERIFYING_MODELS", System.currentTimeMillis())
        val model = modelRepository.getReadyModel(ModelCapability.TRANSCRIPTION)
        if (model == null) {
            _jobStatus.value = AiJobStatus.ModelRequired(
                requiredCapabilities = listOf(ModelCapability.TRANSCRIPTION.name)
            )
            recordingDao.updateTranscriptStatus(recordingId, "MODEL_MISSING", System.currentTimeMillis())
            return
        }
        val diarizationModel = modelRepository.getReadyModel(ModelCapability.DIARIZATION)

        var prepared: PreparedTranscriptionAudio? = null
        try {
            val sourceFingerprint = sha256(sourceFile)
            val existingTranscript = transcriptRepository.findExistingRevision(
                recordingId = recordingId,
                sourceFingerprint = sourceFingerprint,
                modelId = transcriptModelId(model, diarizationModel),
                modelVersion = transcriptModelVersion(model, diarizationModel)
            )
            val job = existingJobId?.let { analysisJobRepository.getJob(it) } ?: analysisJobRepository.enqueueIfAbsent(
                recordingId = recordingId,
                sourceFingerprint = sourceFingerprint,
                requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION),
                options = OfflineProcessingOptions(
                    transcriptionModelId = model.manifest.id,
                    diarizationEnabled = diarizationModel != null,
                    insightsMode = "HEURISTIC",
                    outputLanguage = null
                )
            )
            if (existingTranscript != null) {
                val insights = runInsightsIfAvailable(
                    jobId = job.id,
                    recordingId = recordingId,
                    transcriptRevision = existingTranscript,
                    languageTag = existingTranscript.languageTag
                )
                analysisJobRepository.updateState(
                    jobId = job.id,
                    status = AnalysisJobStatus.COMPLETED,
                    stage = OfflineAnalysisStage.CLEANING_UP,
                    progress = 1f,
                    stageOutputRef = insights?.id ?: existingTranscript.id
                )
                _jobStatus.value = AiJobStatus.Completed(existingTranscript.segments, insights = insights?.insights)
                recordingDao.updateTranscriptStatus(recordingId, "COMPLETE", System.currentTimeMillis())
                return
            }
            _jobStatus.value = AiJobStatus.Queued(job.id)
            recordingDao.updateTranscriptStatus(recordingId, "QUEUED", System.currentTimeMillis())
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.QUEUED,
                stage = OfflineAnalysisStage.QUEUED,
                progress = 0f
            )

            _jobStatus.value = AiJobStatus.PreparingAudio
            recordingDao.updateTranscriptStatus(recordingId, "PROCESSING", System.currentTimeMillis())
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.RUNNING,
                stage = OfflineAnalysisStage.PREPARING_AUDIO,
                progress = 0.15f
            )
            prepared = preprocessor.prepare(sourceFile)
            currentCoroutineContext().ensureActive()

            _jobStatus.value = AiJobStatus.Processing("Transcribing", 0.45f)
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.RUNNING,
                stage = OfflineAnalysisStage.TRANSCRIBING,
                progress = 0.45f,
                modelVersionsJson = """{"${model.manifest.id}":"${model.manifest.version}"}"""
            )
            val output = transcriptionEngine.transcribe(
                TranscriptionInput(
                    pcm16kMonoFile = prepared.pcm16kMonoFile,
                    originalAudioFile = prepared.sourceFile,
                    sourceFingerprint = prepared.sourceFingerprint,
                    languageHint = null,
                    model = model,
                    speechWindows = prepared.speechWindows
                )
            )
            currentCoroutineContext().ensureActive()

            val fusedSegments = if (diarizationModel == null || output.segments.isEmpty()) {
                output.segments
            } else {
                _jobStatus.value = AiJobStatus.Processing("Diarizing speakers", 0.72f)
                val clustering = DiarizationClusteringSettings()
                val existingDiarization = diarizationRepository.findExistingRevision(
                    recordingId = recordingId,
                    sourceFingerprint = prepared.sourceFingerprint,
                    modelId = diarizationModel.manifest.id,
                    modelVersion = diarizationModel.manifest.version,
                    clusteringSettings = clustering
                )
                val diarization = existingDiarization ?: run {
                    analysisJobRepository.updateState(
                        job.id,
                        AnalysisJobStatus.RUNNING,
                        stage = OfflineAnalysisStage.DIARIZING,
                        progress = 0.72f,
                        modelVersionsJson = """{"${model.manifest.id}":"${model.manifest.version}","${diarizationModel.manifest.id}":"${diarizationModel.manifest.version}"}"""
                    )
                    val diarizationOutput = diarizationEngine.diarize(
                        DiarizationInput(
                            pcm16kMonoFile = prepared.pcm16kMonoFile,
                            originalAudioFile = prepared.sourceFile,
                            sourceFingerprint = prepared.sourceFingerprint,
                            model = diarizationModel,
                            clustering = clustering
                        )
                    )
                    currentCoroutineContext().ensureActive()
                    diarizationRepository.publishRevision(
                        recordingId = recordingId,
                        sourceFingerprint = prepared.sourceFingerprint,
                        modelId = diarizationOutput.modelId,
                        modelVersion = diarizationOutput.modelVersion,
                        clusteringSettings = diarizationOutput.clusteringSettings,
                        turns = diarizationOutput.turns,
                        processingDurationMs = diarizationOutput.processingDurationMs,
                        memoryObservationKb = diarizationOutput.memoryObservationKb
                    )
                }
                _jobStatus.value = AiJobStatus.Processing("Aligning speakers", 0.86f)
                analysisJobRepository.updateState(
                    job.id,
                    AnalysisJobStatus.RUNNING,
                    stage = OfflineAnalysisStage.ALIGNING,
                    progress = 0.86f
                )
                TranscriptSpeakerFusion.fuse(output.segments, diarization.turns)
            }

            val revision = transcriptRepository.publishRevision(
                recordingId = recordingId,
                sourceFingerprint = prepared.sourceFingerprint,
                modelId = if (diarizationModel == null) output.modelId else "${output.modelId}+${diarizationModel.manifest.id}",
                modelVersion = if (diarizationModel == null) output.modelVersion else "${output.modelVersion}+${diarizationModel.manifest.version}",
                languageTag = output.languageTag,
                segments = fusedSegments,
                processingDurationMs = output.processingDurationMs,
                memoryObservationKb = output.memoryObservationKb
            )
            val insights = runInsightsIfAvailable(
                jobId = job.id,
                recordingId = recordingId,
                transcriptRevision = revision,
                languageTag = output.languageTag
            )
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.COMPLETED,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = 1f,
                stageOutputRef = insights?.id ?: revision.id,
                modelVersionsJson = """{"${output.modelId}":"${output.modelVersion}"}"""
            )
            recordingDao.updateTranscriptStatus(recordingId, "COMPLETE", System.currentTimeMillis())
            _jobStatus.value = if (insights?.modelId == OfflineAiRuntimeVersions.HYBRID_INSIGHTS_MODEL_ID) {
                AiJobStatus.CompletedWithHeuristicFallback(
                    transcript = revision.segments,
                    insights = insights.insights,
                    reason = "Optional LLM insights were unavailable; heuristic evidence-based insights were preserved."
                )
            } else {
                AiJobStatus.Completed(revision.segments, insights = insights?.insights)
            }
        } catch (cancellation: CancellationException) {
            recordingDao.updateTranscriptStatus(recordingId, "CANCELLED", System.currentTimeMillis())
            recordingDao.updateInsightStatus(recordingId, "CANCELLED", System.currentTimeMillis())
            existingJobId?.let {
                analysisJobRepository.updateState(
                    jobId = it,
                    status = AnalysisJobStatus.CANCELLED,
                    stage = OfflineAnalysisStage.CLEANING_UP,
                    progress = 0f
                )
            }
            _jobStatus.value = AiJobStatus.Cancelled
            throw cancellation
        } catch (error: OfflineAiException) {
            recordingDao.updateTranscriptStatus(recordingId, "FAILED", System.currentTimeMillis())
            recordingDao.updateInsightStatus(recordingId, "FAILED", System.currentTimeMillis())
            existingJobId?.let {
                analysisJobRepository.updateState(
                    jobId = it,
                    status = if (error == OfflineAiException.ModelInitializationFailed || error == OfflineAiException.ProcessingUnavailable) {
                        AnalysisJobStatus.RETRYABLE_FAILURE
                    } else {
                        AnalysisJobStatus.TERMINAL_FAILURE
                    },
                    stage = OfflineAnalysisStage.CLEANING_UP,
                    progress = 0f,
                    errorCode = error::class.simpleName
                )
            }
            _jobStatus.value = AiJobStatus.Failed(
                message = userMessage(error),
                isRetryable = error !is OfflineAiException.DeviceIncompatible && error !is OfflineAiException.ModelMissing
            )
        } finally {
            preprocessor.cleanup(prepared)
            activeJob = null
        }
    }

    fun resetStatus() {
        _jobStatus.value = AiJobStatus.Idle
    }

    fun cancelProcessing() {
        activeJob?.cancel(CancellationException("User cancelled offline processing."))
        _jobStatus.value = AiJobStatus.Cancelled
    }

    private suspend fun runInsightsIfAvailable(
        jobId: String,
        recordingId: String,
        transcriptRevision: com.loomora.core.model.TranscriptRevision,
        languageTag: String?
    ): com.loomora.core.model.InsightRevision? {
        val insightModel = modelRepository.getReadyModel(ModelCapability.INSIGHTS)
        val insightModelId = when (insightModel?.manifest?.runtime) {
            RuntimeKind.LLAMA_CPP -> OfflineAiRuntimeVersions.HYBRID_INSIGHTS_MODEL_ID
            null -> OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID
            else -> insightModel.manifest.id
        }
        val insightModelVersion = when (insightModel?.manifest?.runtime) {
            RuntimeKind.LLAMA_CPP -> OfflineAiRuntimeVersions.HYBRID_INSIGHTS_MODEL_VERSION
            null -> OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_VERSION
            else -> insightModel.manifest.version
        }
        insightRepository.findExistingGeneratedRevision(
            recordingId = recordingId,
            transcriptRevisionId = transcriptRevision.id,
            modelId = insightModelId,
            modelVersion = insightModelVersion
        )?.let { existing ->
            recordingDao.updateInsightStatus(recordingId, "COMPLETE", System.currentTimeMillis())
            return existing
        }
        recordingDao.updateInsightStatus(recordingId, "PROCESSING", System.currentTimeMillis())
        _jobStatus.value = AiJobStatus.Processing("Unloading speech models", 0.9f)
        engineLifecycleManager.close()
        _jobStatus.value = AiJobStatus.Processing("Generating local insights", 0.93f)
        analysisJobRepository.updateState(
            jobId,
            AnalysisJobStatus.RUNNING,
            stage = OfflineAnalysisStage.GENERATING_HEURISTIC_INSIGHTS,
            progress = 0.93f
        )
        val output = meetingInsightEngine.analyze(
            MeetingInsightInput(
                transcriptRevision = transcriptRevision,
                model = insightModel,
                languageTag = languageTag,
                backendPolicy = LiteRtLmBackendPolicy(
                    preferred = listOf(ExecutionBackend.CPU),
                    fallback = listOf(ExecutionBackend.CPU),
                    outputLanguageTag = languageTag
                )
            )
        )
        val revision = insightRepository.publishGeneratedRevision(
            recordingId = recordingId,
            transcriptRevisionId = transcriptRevision.id,
            sourceFingerprint = transcriptRevision.sourceFingerprint,
            modelId = output.modelId,
            modelVersion = output.modelVersion,
            languageTag = output.languageTag,
            insights = output.insights,
            checkpoints = output.chunkCheckpoints,
            modelSizeBytes = output.modelSizeBytes,
            loadTimeMs = output.loadTimeMs,
            generationTimeMs = output.generationTimeMs,
            memoryObservationKb = output.memoryObservationKb,
            generationMode = output.generationMode,
            completionQuality = output.completionQuality,
            fallbackReason = output.fallbackReason
        )
        val insightStatus = if (output.usedHeuristicFallback) {
            "COMPLETED_WITH_HEURISTIC_FALLBACK"
        } else {
            "COMPLETE"
        }
        recordingDao.updateInsightStatus(recordingId, insightStatus, System.currentTimeMillis())
        return revision
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private fun transcriptModelId(
        transcriptionModel: OfflineModelRecord,
        diarizationModel: OfflineModelRecord?
    ): String {
        return if (diarizationModel == null) {
            transcriptionModel.manifest.id
        } else {
            "${transcriptionModel.manifest.id}+${diarizationModel.manifest.id}"
        }
    }

    private fun transcriptModelVersion(
        transcriptionModel: OfflineModelRecord,
        diarizationModel: OfflineModelRecord?
    ): String {
        return if (diarizationModel == null) {
            transcriptionModel.manifest.version
        } else {
            "${transcriptionModel.manifest.version}+${diarizationModel.manifest.version}"
        }
    }

    private fun userMessage(error: OfflineAiException): String {
        return when (error) {
            OfflineAiException.FileMissing -> "The local audio file is missing."
            OfflineAiException.FileCorrupt -> "The local audio file could not be decoded for offline transcription."
            OfflineAiException.ModelMissing -> "The offline transcription model is missing."
            OfflineAiException.ModelFileMissing -> "The installed transcription model file is missing."
            OfflineAiException.DeviceIncompatible -> "This device is not compatible with the selected offline model."
            OfflineAiException.ProcessingCancelled -> "Offline transcription was cancelled."
            OfflineAiException.ProcessingUnavailable -> "The sherpa-onnx offline transcription runtime is not available yet."
            OfflineAiException.ModelInitializationFailed -> "The offline transcription model could not be initialized."
            OfflineAiException.InsightParseFailed -> "The local insight model returned invalid structured output. You can retry."
            OfflineAiException.InsightSemanticInvalid -> "The local insight model returned unsupported content, so heuristic insights were preserved."
            OfflineAiException.ModelChecksumMismatch -> "The installed model checksum is invalid."
            OfflineAiException.ImportInterrupted -> "Model import was interrupted."
        }
    }
}
