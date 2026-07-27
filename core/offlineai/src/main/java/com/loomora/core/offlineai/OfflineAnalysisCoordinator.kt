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
    private val recordingDao: RecordingDao,
    private val preprocessor: AudioTranscriptionPreprocessor,
    private val transcriptionEngine: LocalTranscriptionEngine
) {
    private val _jobStatus = MutableStateFlow<AiJobStatus>(AiJobStatus.Idle)
    val jobStatus: StateFlow<AiJobStatus> = _jobStatus.asStateFlow()
    private var activeJob: Job? = null

    suspend fun processAudio(recordingId: String, audioFileUri: String) {
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

        var prepared: PreparedTranscriptionAudio? = null
        try {
            val sourceFingerprint = sha256(sourceFile)
            transcriptRepository.findExistingRevision(
                recordingId = recordingId,
                sourceFingerprint = sourceFingerprint,
                modelId = model.manifest.id,
                modelVersion = model.manifest.version
            )?.let { existing ->
                _jobStatus.value = AiJobStatus.Completed(existing.segments, insights = null)
                recordingDao.updateTranscriptStatus(recordingId, "COMPLETE", System.currentTimeMillis())
                return
            }

            val job = analysisJobRepository.enqueueIfAbsent(
                recordingId = recordingId,
                sourceFingerprint = sourceFingerprint,
                requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION)
            )
            _jobStatus.value = AiJobStatus.Queued(job.id)
            recordingDao.updateTranscriptStatus(recordingId, "QUEUED", System.currentTimeMillis())
            analysisJobRepository.updateState(job.id, AnalysisJobStatus.QUEUED, 0f)

            _jobStatus.value = AiJobStatus.PreparingAudio
            recordingDao.updateTranscriptStatus(recordingId, "PROCESSING", System.currentTimeMillis())
            analysisJobRepository.updateState(job.id, AnalysisJobStatus.PREPARING_AUDIO, 0.15f)
            prepared = preprocessor.prepare(sourceFile)
            currentCoroutineContext().ensureActive()

            _jobStatus.value = AiJobStatus.Processing("Transcribing", 0.45f)
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.TRANSCRIBING,
                0.45f,
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

            val revision = transcriptRepository.publishRevision(
                recordingId = recordingId,
                sourceFingerprint = prepared.sourceFingerprint,
                modelId = output.modelId,
                modelVersion = output.modelVersion,
                languageTag = output.languageTag,
                segments = output.segments,
                processingDurationMs = output.processingDurationMs,
                memoryObservationKb = output.memoryObservationKb
            )
            analysisJobRepository.updateState(
                job.id,
                AnalysisJobStatus.COMPLETED,
                1f,
                stageOutputRef = revision.id,
                modelVersionsJson = """{"${output.modelId}":"${output.modelVersion}"}"""
            )
            recordingDao.updateTranscriptStatus(recordingId, "COMPLETE", System.currentTimeMillis())
            _jobStatus.value = AiJobStatus.Completed(revision.segments, insights = null)
        } catch (cancellation: CancellationException) {
            recordingDao.updateTranscriptStatus(recordingId, "CANCELLED", System.currentTimeMillis())
            _jobStatus.value = AiJobStatus.Cancelled
            throw cancellation
        } catch (error: OfflineAiException) {
            recordingDao.updateTranscriptStatus(recordingId, "FAILED", System.currentTimeMillis())
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
        activeJob?.cancel(CancellationException("User cancelled offline transcription."))
        _jobStatus.value = AiJobStatus.Cancelled
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
            OfflineAiException.ModelChecksumMismatch -> "The installed model checksum is invalid."
            OfflineAiException.ImportInterrupted -> "Model import was interrupted."
        }
    }
}
