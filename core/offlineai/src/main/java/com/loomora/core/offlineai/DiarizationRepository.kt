package com.loomora.core.offlineai

import com.loomora.core.database.dao.DiarizationDao
import com.loomora.core.database.entity.DiarizationRevisionEntity
import com.loomora.core.database.entity.SpeakerAliasEntity
import com.loomora.core.database.entity.SpeakerTurnEntity
import com.loomora.core.model.DiarizationRevision
import com.loomora.core.model.SpeakerAlias
import com.loomora.core.model.SpeakerTurn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiarizationRepository @Inject constructor(
    private val diarizationDao: DiarizationDao,
    private val json: Json
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLatestDiarization(recordingId: String): Flow<DiarizationRevision?> {
        return diarizationDao.observeLatestRevision(recordingId).flatMapLatest { revision ->
            if (revision == null) {
                flowOf(null)
            } else {
                diarizationDao.observeTurnsForRevision(revision.id).map { turns ->
                    revision.toModel(turns)
                }
            }
        }
    }

    fun observeAliases(recordingId: String): Flow<List<SpeakerAlias>> {
        return diarizationDao.observeAliases(recordingId).map { aliases ->
            aliases.map { it.toModel() }
        }
    }

    suspend fun findExistingRevision(
        recordingId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String,
        clusteringSettings: DiarizationClusteringSettings
    ): DiarizationRevision? {
        val clusteringJson = clusteringSettings.toStableJson()
        val revision = diarizationDao.getRevisionByIdentity(
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.DIARIZATION_PIPELINE_VERSION,
            modelId = modelId,
            modelVersion = modelVersion,
            clusteringSettingsHash = sha256(clusteringJson)
        ) ?: return null
        return revision.toModel(diarizationDao.getTurnsForRevisionSync(revision.id))
    }

    suspend fun publishRevision(
        recordingId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String,
        clusteringSettings: DiarizationClusteringSettings,
        turns: List<SpeakerTurn>,
        processingDurationMs: Long,
        memoryObservationKb: Long?
    ): DiarizationRevision {
        val clusteringJson = clusteringSettings.toStableJson()
        val revisionId = stableRevisionId(
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            modelId = modelId,
            modelVersion = modelVersion,
            clusteringSettingsHash = sha256(clusteringJson)
        )
        val normalized = normalizeTurns(turns)
        val now = System.currentTimeMillis()
        val revision = DiarizationRevisionEntity(
            id = revisionId,
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.DIARIZATION_PIPELINE_VERSION,
            modelId = modelId,
            modelVersion = modelVersion,
            clusteringSettings = clusteringJson,
            clusteringSettingsHash = sha256(clusteringJson),
            status = "COMPLETE",
            turnCount = normalized.size,
            processingDurationMs = processingDurationMs,
            memoryObservationKb = memoryObservationKb,
            createdAt = now,
            updatedAt = now
        )
        val entities = normalized.mapIndexed { index, turn ->
            SpeakerTurnEntity(
                id = stableTurnId(revisionId, index, turn.startMs, turn.endMs, turn.speakerLabel),
                revisionId = revisionId,
                recordingId = recordingId,
                orderIndex = index,
                startMs = turn.startMs,
                endMs = turn.endMs,
                speakerLabel = turn.speakerLabel,
                speakerIndex = turn.speakerIndex,
                confidence = turn.confidence,
                isOverlapped = turn.isOverlapped,
                isUncertain = turn.isUncertain,
                alternateSpeakerLabelsJson = json.encodeToString(turn.alternateSpeakerLabels)
            )
        }
        diarizationDao.replaceRevisionTurns(revision, entities)
        return revision.toModel(entities)
    }

    suspend fun renameSpeaker(recordingId: String, genericLabel: String, displayName: String) {
        if (genericLabel.isBlank() || displayName.isBlank()) return
        diarizationDao.upsertAlias(
            SpeakerAliasEntity(
                recordingId = recordingId,
                genericLabel = genericLabel,
                displayName = displayName.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun normalizeTurns(turns: List<SpeakerTurn>): List<SpeakerTurn> {
        return turns.sortedWith(compareBy({ it.startMs }, { it.endMs }, { it.speakerIndex }))
            .filter { it.endMs > it.startMs && it.speakerLabel.isNotBlank() }
            .map {
                it.copy(
                    id = "",
                    revisionId = "",
                    recordingId = "",
                    speakerLabel = it.speakerLabel.trim()
                )
            }
    }

    private fun DiarizationRevisionEntity.toModel(turns: List<SpeakerTurnEntity>): DiarizationRevision {
        return DiarizationRevision(
            id = id,
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = pipelineVersion,
            modelId = modelId,
            modelVersion = modelVersion,
            clusteringSettings = clusteringSettings,
            createdAt = createdAt,
            turns = turns.sortedBy { it.orderIndex }.map { it.toModel() }
        )
    }

    private fun SpeakerTurnEntity.toModel(): SpeakerTurn {
        return SpeakerTurn(
            id = id,
            revisionId = revisionId,
            recordingId = recordingId,
            startMs = startMs,
            endMs = endMs,
            speakerLabel = speakerLabel,
            speakerIndex = speakerIndex,
            confidence = confidence,
            isOverlapped = isOverlapped,
            isUncertain = isUncertain,
            alternateSpeakerLabels = runCatching {
                json.decodeFromString<List<String>>(alternateSpeakerLabelsJson)
            }.getOrDefault(emptyList())
        )
    }

    private fun SpeakerAliasEntity.toModel(): SpeakerAlias {
        return SpeakerAlias(
            recordingId = recordingId,
            genericLabel = genericLabel,
            displayName = displayName,
            updatedAt = updatedAt
        )
    }

    private fun DiarizationClusteringSettings.toStableJson(): String {
        return "{" +
            "\"numClusters\":$numClusters," +
            "\"threshold\":$threshold," +
            "\"minDurationOnSec\":$minDurationOnSec," +
            "\"minDurationOffSec\":$minDurationOffSec," +
            "\"numThreads\":$numThreads," +
            "\"provider\":\"$provider\"" +
            "}"
    }

    private fun stableRevisionId(
        recordingId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String,
        clusteringSettingsHash: String
    ): String = sha256(
        listOf(
            recordingId,
            sourceFingerprint,
            OfflineAiRuntimeVersions.DIARIZATION_PIPELINE_VERSION,
            modelId,
            modelVersion,
            clusteringSettingsHash
        ).joinToString("|")
    ).take(32)

    private fun stableTurnId(
        revisionId: String,
        orderIndex: Int,
        startMs: Long,
        endMs: Long,
        speakerLabel: String
    ): String = sha256("$revisionId|$orderIndex|$startMs|$endMs|$speakerLabel").take(32)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(value.toByteArray())
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}
