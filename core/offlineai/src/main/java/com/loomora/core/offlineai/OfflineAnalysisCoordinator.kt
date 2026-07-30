package com.loomora.core.offlineai

import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.AiProcessingStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

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
        processAudioInternal(recordingId, audioFileUri, existingJobId = null, existingFingerprint = null)
    }

    suspend fun processAudioForJob(jobId: String, recordingId: String, audioFileUri: String, sourceFingerprint: String? = null) {
        processAudioInternal(recordingId, audioFileUri, existingJobId = jobId, existingFingerprint = sourceFingerprint)
    }

    private suspend fun processAudioInternal(
        recordingId: String,
        audioFileUri: String,
        existingJobId: String?,
        existingFingerprint: String?
    ) {
        activeJob = currentCoroutineContext()[Job]
        val totalMark = TimeSource.Monotonic.markNow()
        var timings = OfflineAiStageTimings()
        var timingJobId: String? = existingJobId
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
        val existingJob = existingJobId?.let { analysisJobRepository.getJob(it) }
        val requestedOptions = existingJob?.let(analysisJobRepository::optionsFor)
        val requestedModelId = requestedOptions?.transcriptionModelId
        val model = if (requestedModelId == null) {
            modelRepository.getReadyModel(ModelCapability.TRANSCRIPTION)
        } else {
            modelRepository.getReadyModel(requestedModelId)
        }
        if (model == null) {
            _jobStatus.value = AiJobStatus.ModelRequired(
                requiredCapabilities = listOf(ModelCapability.TRANSCRIPTION.name)
            )
            recordingDao.updateTranscriptStatus(recordingId, "MODEL_MISSING", System.currentTimeMillis())
            return
        }
        val diarizationModel = modelRepository.getReadyModel(ModelCapability.DIARIZATION)
        val vadModel = modelRepository.getReadyModel(ModelCapability.VOICE_ACTIVITY_DETECTION)

        var prepared: PreparedTranscriptionAudio? = null
        var publishedTranscript: com.loomora.core.model.TranscriptRevision? = null
        try {
            val fingerprintMark = TimeSource.Monotonic.markNow()
            val sourceFingerprint = existingFingerprint ?: preprocessor.fingerprint(sourceFile)
            timings = timings.copy(
                fingerprintMs = if (existingFingerprint == null) fingerprintMark.elapsedNow().inWholeMilliseconds else 0L
            )
            val existingTranscript = transcriptRepository.findExistingRevision(
                recordingId = recordingId,
                sourceFingerprint = sourceFingerprint,
                modelId = transcriptModelId(model, diarizationModel),
                modelVersion = transcriptModelVersion(model, diarizationModel)
            )
            val effectiveOptions = requestedOptions ?: OfflineProcessingOptions(
                transcriptionModelId = model.manifest.id,
                diarizationEnabled = diarizationModel != null,
                insightsMode = "HEURISTIC",
                outputLanguage = null
            )
            val job = existingJob ?: analysisJobRepository.enqueueIfAbsent(
                recordingId = recordingId,
                sourceFingerprint = sourceFingerprint,
                requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION),
                options = effectiveOptions
            )
            timingJobId = job.id
            if (existingTranscript != null && !effectiveOptions.forceReanalysis) {
                val insights = if (effectiveOptions.insightsMode == "NONE") null else runInsightsIfAvailable(
                    jobId = job.id,
                    recordingId = recordingId,
                    transcriptRevision = existingTranscript,
                    languageTag = existingTranscript.languageTag,
                    forceRegeneration = false
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
            prepared = if (vadModel == null) {
                preprocessor.prepare(sourceFile, sourceFingerprint)
            } else {
                preprocessor.prepare(sourceFile, sourceFingerprint, vadModel)
            }
            timings = timings.copy(
                decodeAndResampleMs = prepared.decodeAndResampleMs,
                speechDetectionMs = prepared.speechDetectionMs
            )
            currentCoroutineContext().ensureActive()

            _jobStatus.value = AiJobStatus.Processing(AiProcessingStage.TRANSCRIBING, 0.45f)
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.RUNNING,
                stage = OfflineAnalysisStage.TRANSCRIBING,
                progress = 0.45f,
                modelVersionsJson = """{"${model.manifest.id}":"${model.manifest.version}"}"""
            )
            val transcriptionMark = TimeSource.Monotonic.markNow()
            engineLifecycleManager.register(transcriptionEngine)
            val output = transcriptionEngine.transcribe(
                TranscriptionInput(
                    pcm16kMonoFile = prepared.pcm16kMonoFile,
                    originalAudioFile = prepared.sourceFile,
                    sourceFingerprint = prepared.sourceFingerprint,
                    languageHint = effectiveOptions.outputLanguage,
                    model = model,
                    speechWindows = prepared.speechWindows,
                    performanceProfile = effectiveOptions.performanceProfile
                )
            )
            timings = timings.copy(
                transcriptionMs = transcriptionMark.elapsedNow().inWholeMilliseconds
            )
            currentCoroutineContext().ensureActive()
            val cleanedSegments = TranscriptHallucinationFilter.clean(output.segments)

            val partialRevision = transcriptRepository.publishRevision(
                recordingId = recordingId,
                sourceFingerprint = prepared.sourceFingerprint,
                modelId = output.modelId,
                modelVersion = output.modelVersion,
                languageTag = output.languageTag,
                segments = cleanedSegments,
                processingDurationMs = output.processingDurationMs,
                memoryObservationKb = output.memoryObservationKb
            )
            publishedTranscript = partialRevision
            recordingDao.updateTranscriptStatus(recordingId, "AVAILABLE", System.currentTimeMillis())
            _jobStatus.value = AiJobStatus.Partial(partialRevision.segments, progress = 0.68f)

            val fusedSegments = if (!effectiveOptions.diarizationEnabled || diarizationModel == null || cleanedSegments.isEmpty()) {
                cleanedSegments
            } else {
                _jobStatus.value = AiJobStatus.Processing(AiProcessingStage.DIARIZING, 0.72f)
                val clustering = DiarizationClusteringSettings()
                val existingDiarization = if (effectiveOptions.forceReanalysis) null else diarizationRepository.findExistingRevision(
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
                    val diarizationMark = TimeSource.Monotonic.markNow()
                    engineLifecycleManager.register(diarizationEngine)
                    val diarizationOutput = diarizationEngine.diarize(
                        DiarizationInput(
                            pcm16kMonoFile = prepared.pcm16kMonoFile,
                            originalAudioFile = prepared.sourceFile,
                            sourceFingerprint = prepared.sourceFingerprint,
                            model = diarizationModel,
                            clustering = clustering
                        )
                    )
                    timings = timings.copy(
                        diarizationMs = diarizationMark.elapsedNow().inWholeMilliseconds
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
                _jobStatus.value = AiJobStatus.Processing(AiProcessingStage.ALIGNING, 0.86f)
                analysisJobRepository.updateState(
                    job.id,
                    AnalysisJobStatus.RUNNING,
                    stage = OfflineAnalysisStage.ALIGNING,
                    progress = 0.86f
                )
                val alignmentMark = TimeSource.Monotonic.markNow()
                TranscriptSpeakerFusion.fuse(cleanedSegments, diarization.turns).also {
                    timings = timings.copy(
                        alignmentMs = alignmentMark.elapsedNow().inWholeMilliseconds
                    )
                }
            }

            val revision = if (!effectiveOptions.diarizationEnabled || diarizationModel == null) {
                partialRevision
            } else transcriptRepository.publishRevision(
                recordingId = recordingId,
                sourceFingerprint = prepared.sourceFingerprint,
                modelId = "${output.modelId}+${diarizationModel.manifest.id}",
                modelVersion = "${output.modelVersion}+${diarizationModel.manifest.version}",
                languageTag = output.languageTag,
                segments = fusedSegments,
                processingDurationMs = output.processingDurationMs,
                memoryObservationKb = output.memoryObservationKb
            )
            publishedTranscript = revision
            val insightMark = TimeSource.Monotonic.markNow()
            val insights = if (effectiveOptions.insightsMode == "NONE") null else runInsightsIfAvailable(
                jobId = job.id,
                recordingId = recordingId,
                transcriptRevision = revision,
                languageTag = output.languageTag,
                forceRegeneration = effectiveOptions.forceReanalysis
            )
            timings = timings.copy(
                heuristicInsightsMs = insightMark.elapsedNow().inWholeMilliseconds
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
            recordingDao.updateTranscriptStatus(
                recordingId,
                if (publishedTranscript == null) "CANCELLED" else "AVAILABLE",
                System.currentTimeMillis()
            )
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
            recordingDao.updateTranscriptStatus(
                recordingId,
                if (publishedTranscript == null) "FAILED" else "AVAILABLE",
                System.currentTimeMillis()
            )
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
                isRetryable = error !is OfflineAiException.DeviceIncompatible && error !is OfflineAiException.ModelMissing,
                preservedTranscript = publishedTranscript?.segments
            )
        } finally {
            timingJobId?.let { jobId ->
                timings = timings.copy(totalMs = totalMark.elapsedNow().inWholeMilliseconds)
                try {
                    analysisJobRepository.updateTimings(jobId, timings)
                } catch (_: Exception) {
                    // Timing persistence must never mask the analysis result.
                }
            }
            preprocessor.cleanup(prepared)
            engineLifecycleManager.close()
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
        languageTag: String?,
        forceRegeneration: Boolean
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
        if (!forceRegeneration) {
            insightRepository.findExistingGeneratedRevision(
                recordingId = recordingId,
                transcriptRevisionId = transcriptRevision.id,
                modelId = insightModelId,
                modelVersion = insightModelVersion
            )?.let { existing ->
                recordingDao.updateInsightStatus(recordingId, "COMPLETE", System.currentTimeMillis())
                return existing
            }
        }
        recordingDao.updateInsightStatus(recordingId, "PROCESSING", System.currentTimeMillis())
        _jobStatus.value = AiJobStatus.Processing(AiProcessingStage.GENERATING_INSIGHTS, 0.93f)
        analysisJobRepository.updateState(
            jobId,
            AnalysisJobStatus.RUNNING,
            stage = OfflineAnalysisStage.GENERATING_HEURISTIC_INSIGHTS,
            progress = 0.93f
        )
        engineLifecycleManager.register(meetingInsightEngine)
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
