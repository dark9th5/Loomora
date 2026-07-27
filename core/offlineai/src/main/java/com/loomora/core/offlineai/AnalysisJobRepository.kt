package com.loomora.core.offlineai

import com.loomora.core.database.dao.AnalysisJobDao
import com.loomora.core.database.entity.AnalysisJobEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        requestedCapabilities: Set<ModelCapability>
    ): AnalysisJobEntity {
        val logicalKey = listOf(
            recordingId,
            sourceFingerprint,
            OfflineAiRuntimeVersions.PIPELINE_VERSION,
            requestedCapabilities.sortedBy { it.name }.joinToString(separator = ",") { it.name }
        ).joinToString(separator = "|")

        analysisJobDao.getJobByLogicalKey(logicalKey)?.let { return it }

        val now = System.currentTimeMillis()
        val job = AnalysisJobEntity(
            id = UUID.randomUUID().toString(),
            logicalKey = logicalKey,
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.PIPELINE_VERSION,
            requestedOptionsJson = json.encodeToString(requestedCapabilities.sortedBy { it.name }.map { it.name }),
            status = AnalysisJobStatus.QUEUED.name,
            progress = 0f,
            attempt = 0,
            stageOutputRef = null,
            modelVersionsJson = "{}",
            errorCode = null,
            createdAt = now,
            updatedAt = now
        )
        analysisJobDao.upsertJob(job)
        return job
    }

    suspend fun updateState(
        jobId: String,
        status: AnalysisJobStatus,
        progress: Float,
        stageOutputRef: String? = null,
        modelVersionsJson: String = "{}",
        errorCode: String? = null
    ) {
        analysisJobDao.updateJobState(
            id = jobId,
            status = status.name,
            progress = progress,
            stageOutputRef = stageOutputRef,
            modelVersionsJson = modelVersionsJson,
            errorCode = errorCode,
            updatedAt = System.currentTimeMillis()
        )
    }
}
