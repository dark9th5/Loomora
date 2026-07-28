package com.loomora.core.offlineai

import com.loomora.core.database.dao.InsightDao
import com.loomora.core.database.entity.InsightChunkCheckpointEntity
import com.loomora.core.database.entity.InsightRevisionEntity
import com.loomora.core.model.AiInsights
import com.loomora.core.model.InsightRevision
import com.loomora.core.model.InsightRevisionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao,
    private val json: Json
) {
    fun observeLatestInsight(recordingId: String): Flow<InsightRevision?> {
        return insightDao.observeLatestRevision(recordingId).map { it?.toModel() }
    }

    suspend fun findExistingGeneratedRevision(
        recordingId: String,
        transcriptRevisionId: String,
        modelId: String,
        modelVersion: String
    ): InsightRevision? {
        return insightDao.getRevisionByIdentity(
            recordingId = recordingId,
            transcriptRevisionId = transcriptRevisionId,
            pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
            promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
            schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
            modelId = modelId,
            modelVersion = modelVersion,
            kind = InsightRevisionKind.GENERATED.name
        )?.toModel()
    }

    suspend fun publishGeneratedRevision(
        recordingId: String,
        transcriptRevisionId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String,
        languageTag: String?,
        insights: AiInsights,
        checkpoints: List<InsightChunkCheckpoint>,
        modelSizeBytes: Long,
        loadTimeMs: Long,
        generationTimeMs: Long,
        memoryObservationKb: Long?,
        generationMode: InsightGenerationMode = InsightGenerationMode.HEURISTIC,
        completionQuality: InsightCompletionQuality = InsightCompletionQuality.EXTRACTIVE_ONLY,
        fallbackReason: String? = null
    ): InsightRevision {
        return publishRevision(
            recordingId = recordingId,
            transcriptRevisionId = transcriptRevisionId,
            sourceFingerprint = sourceFingerprint,
            modelId = modelId,
            modelVersion = modelVersion,
            languageTag = languageTag,
            kind = InsightRevisionKind.GENERATED,
            insights = insights,
            checkpoints = checkpoints,
            modelSizeBytes = modelSizeBytes,
            loadTimeMs = loadTimeMs,
            generationTimeMs = generationTimeMs,
            memoryObservationKb = memoryObservationKb,
            generationMode = generationMode.name,
            completionQuality = completionQuality.name,
            fallbackReason = fallbackReason
        )
    }

    suspend fun publishUserEditedRevision(
        base: InsightRevision,
        editedInsights: AiInsights
    ): InsightRevision {
        return publishRevision(
            recordingId = base.recordingId,
            transcriptRevisionId = base.transcriptRevisionId,
            sourceFingerprint = base.sourceFingerprint,
            modelId = base.modelId,
            modelVersion = base.modelVersion,
            languageTag = base.languageTag,
            kind = InsightRevisionKind.USER_EDITED,
            insights = editedInsights,
            checkpoints = emptyList(),
            modelSizeBytes = base.modelSizeBytes,
            loadTimeMs = 0L,
            generationTimeMs = 0L,
            memoryObservationKb = null,
            generationMode = base.generationMode,
            completionQuality = base.completionQuality,
            fallbackReason = base.fallbackReason
        )
    }

    private suspend fun publishRevision(
        recordingId: String,
        transcriptRevisionId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String,
        languageTag: String?,
        kind: InsightRevisionKind,
        insights: AiInsights,
        checkpoints: List<InsightChunkCheckpoint>,
        modelSizeBytes: Long,
        loadTimeMs: Long,
        generationTimeMs: Long,
        memoryObservationKb: Long?,
        generationMode: String,
        completionQuality: String,
        fallbackReason: String?
    ): InsightRevision {
        val revisionId = stableRevisionId(recordingId, transcriptRevisionId, modelId, modelVersion, kind)
        val now = System.currentTimeMillis()
        val entity = InsightRevisionEntity(
            id = revisionId,
            recordingId = recordingId,
            transcriptRevisionId = transcriptRevisionId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
            promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
            schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
            modelId = modelId,
            modelVersion = modelVersion,
            languageTag = languageTag,
            kind = kind.name,
            status = "COMPLETE",
            insightsJson = json.encodeToString(insights),
            modelSizeBytes = modelSizeBytes,
            loadTimeMs = loadTimeMs,
            generationTimeMs = generationTimeMs,
            memoryObservationKb = memoryObservationKb,
            generationMode = generationMode,
            completionQuality = completionQuality,
            fallbackReason = fallbackReason,
            createdAt = now,
            updatedAt = now
        )
        val checkpointEntities = checkpoints.map {
            InsightChunkCheckpointEntity(
                id = sha256("$revisionId|${it.chunkIndex}|${it.startMs}|${it.endMs}").take(32),
                revisionId = revisionId,
                chunkIndex = it.chunkIndex,
                startMs = it.startMs,
                endMs = it.endMs,
                segmentIdsJson = json.encodeToString(it.segmentIds),
                promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
                schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
                outputJson = it.outputJson,
                createdAt = now
            )
        }
        insightDao.replaceRevision(entity, checkpointEntities)
        return entity.toModel()
    }

    private fun InsightRevisionEntity.toModel(): InsightRevision {
        return InsightRevision(
            id = id,
            recordingId = recordingId,
            transcriptRevisionId = transcriptRevisionId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = pipelineVersion,
            promptVersion = promptVersion,
            schemaVersion = schemaVersion,
            modelId = modelId,
            modelVersion = modelVersion,
            languageTag = languageTag,
            kind = InsightRevisionKind.valueOf(kind),
            createdAt = createdAt,
            insights = json.decodeFromString(insightsJson),
            modelSizeBytes = modelSizeBytes,
            loadTimeMs = loadTimeMs,
            generationTimeMs = generationTimeMs,
            memoryObservationKb = memoryObservationKb,
            generationMode = generationMode,
            completionQuality = completionQuality,
            fallbackReason = fallbackReason
        )
    }

    private fun stableRevisionId(
        recordingId: String,
        transcriptRevisionId: String,
        modelId: String,
        modelVersion: String,
        kind: InsightRevisionKind
    ): String = sha256(
        listOf(
            recordingId,
            transcriptRevisionId,
            OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
            OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
            OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
            modelId,
            modelVersion,
            kind.name
        ).joinToString("|")
    ).take(32)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(value.toByteArray())
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}
