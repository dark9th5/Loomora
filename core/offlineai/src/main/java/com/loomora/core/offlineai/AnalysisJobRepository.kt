package com.loomora.core.offlineai

import com.loomora.core.database.dao.AnalysisJobDao
import com.loomora.core.database.entity.AnalysisJobEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisJobRepository @Inject constructor(
    private val analysisJobDao: AnalysisJobDao,
    private val json: Json
) {
    fun observeJobsForRecording(recordingId: String): Flow<List<AnalysisJobEntity>> {
        return analysisJobDao.observeJobsForRecording(recordingId)
    }

    suspend fun enqueueIfAbsent(
        recordingId: String,
        sourceFingerprint: String,
        requestedCapabilities: Set<ModelCapability>,
        options: OfflineProcessingOptions = OfflineProcessingOptions()
    ): AnalysisJobEntity {
        val canonicalOptions = options.canonical()
        val requestedOptionsJson = json.encodeToString(canonicalOptions)
        val logicalKey = logicalKey(recordingId, sourceFingerprint, requestedOptionsJson)

        analysisJobDao.getJobByLogicalKey(logicalKey)?.let { return it }

        val now = System.currentTimeMillis()
        val job = AnalysisJobEntity(
            id = UUID.randomUUID().toString(),
            logicalKey = logicalKey,
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.PIPELINE_VERSION,
            requestedOptionsJson = requestedOptionsJson,
            status = AnalysisJobStatus.QUEUED.name,
            stage = OfflineAnalysisStage.QUEUED.name,
            progress = 0f,
            attempt = 0,
            workRequestId = null,
            checkpointRef = null,
            stageOutputRef = null,
            modelVersionsJson = "{}",
            errorCode = null,
            skipReason = null,
            fallbackReason = null,
            startedAt = null,
            finishedAt = null,
            createdAt = now,
            updatedAt = now
        )
        analysisJobDao.upsertJob(job)
        return job
    }

    suspend fun updateState(
        jobId: String,
        status: AnalysisJobStatus,
        stage: OfflineAnalysisStage = stageFromStatus(status),
        progress: Float,
        checkpointRef: String? = null,
        stageOutputRef: String? = null,
        modelVersionsJson: String = "{}",
        errorCode: String? = null,
        skipReason: String? = null,
        fallbackReason: String? = null
    ) {
        val existing = analysisJobDao.getJobById(jobId)
        val now = System.currentTimeMillis()
        analysisJobDao.updateJobState(
            id = jobId,
            status = status.name,
            stage = stage.name,
            progress = progress,
            checkpointRef = checkpointRef ?: existing?.checkpointRef,
            stageOutputRef = stageOutputRef,
            modelVersionsJson = modelVersionsJson,
            errorCode = errorCode,
            skipReason = skipReason,
            fallbackReason = fallbackReason,
            startedAt = existing?.startedAt ?: if (status == AnalysisJobStatus.RUNNING) now else null,
            finishedAt = if (status in terminalStatuses) now else existing?.finishedAt,
            updatedAt = now
        )
    }

    suspend fun getJob(jobId: String): AnalysisJobEntity? = analysisJobDao.getJobById(jobId)

    suspend fun updateWorkRequestId(jobId: String, workRequestId: String) {
        analysisJobDao.updateWorkRequestId(jobId, workRequestId, System.currentTimeMillis())
    }

    suspend fun requestCancel(jobId: String) {
        analysisJobDao.requestCancel(jobId, System.currentTimeMillis())
    }

    suspend fun reconcileRunningJobs() {
        analysisJobDao.reconcileRunningToQueued(System.currentTimeMillis())
    }

    private fun logicalKey(
        recordingId: String,
        sourceFingerprint: String,
        requestedOptionsJson: String
    ): String {
        return sha256(
            listOf(
                recordingId,
                sourceFingerprint,
                OfflineAiRuntimeVersions.PIPELINE_VERSION,
                requestedOptionsJson
            ).joinToString("|")
        )
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(value.toByteArray())
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private fun stageFromStatus(status: AnalysisJobStatus): OfflineAnalysisStage {
        return when (status) {
            AnalysisJobStatus.PREPARING_AUDIO -> OfflineAnalysisStage.PREPARING_AUDIO
            AnalysisJobStatus.ENHANCING -> OfflineAnalysisStage.ENHANCING
            AnalysisJobStatus.DETECTING_SPEECH -> OfflineAnalysisStage.DETECTING_SPEECH
            AnalysisJobStatus.TRANSCRIBING -> OfflineAnalysisStage.TRANSCRIBING
            AnalysisJobStatus.DIARIZING -> OfflineAnalysisStage.DIARIZING
            AnalysisJobStatus.ALIGNING -> OfflineAnalysisStage.ALIGNING
            AnalysisJobStatus.SUMMARIZING_CHUNKS,
            AnalysisJobStatus.SYNTHESIZING -> OfflineAnalysisStage.GENERATING_HEURISTIC_INSIGHTS
            AnalysisJobStatus.VALIDATING -> OfflineAnalysisStage.VALIDATING
            AnalysisJobStatus.PUBLISHING -> OfflineAnalysisStage.PUBLISHING
            AnalysisJobStatus.CLEANING_UP -> OfflineAnalysisStage.CLEANING_UP
            else -> OfflineAnalysisStage.QUEUED
        }
    }

    private companion object {
        val terminalStatuses = setOf(
            AnalysisJobStatus.CANCELLED,
            AnalysisJobStatus.COMPLETED,
            AnalysisJobStatus.TERMINAL_FAILURE,
            AnalysisJobStatus.INVALIDATED
        )
    }
}
