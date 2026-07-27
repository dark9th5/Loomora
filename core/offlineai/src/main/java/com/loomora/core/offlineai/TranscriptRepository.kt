package com.loomora.core.offlineai

import com.loomora.core.database.dao.TranscriptDao
import com.loomora.core.database.entity.TranscriptRevisionEntity
import com.loomora.core.database.entity.TranscriptSegmentEntity
import com.loomora.core.model.TranscriptRevision
import com.loomora.core.model.TranscriptSegment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLatestTranscript(recordingId: String): Flow<TranscriptRevision?> {
        return transcriptDao.observeLatestRevision(recordingId).flatMapLatest { revision ->
            if (revision == null) {
                flowOf(null)
            } else {
                transcriptDao.observeSegmentsForRevision(revision.id).map { segments ->
                    revision.toModel(segments)
                }
            }
        }
    }

    suspend fun findExistingRevision(
        recordingId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String
    ): TranscriptRevision? {
        val revision = transcriptDao.getRevisionByIdentity(
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
            modelId = modelId,
            modelVersion = modelVersion
        ) ?: return null
        return revision.toModel(transcriptDao.getSegmentsForRevisionSync(revision.id))
    }

    suspend fun publishRevision(
        recordingId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String,
        languageTag: String?,
        segments: List<TranscriptSegment>,
        processingDurationMs: Long,
        memoryObservationKb: Long?
    ): TranscriptRevision {
        val normalized = normalizeSegments(segments)
        val revisionId = stableRevisionId(
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            modelId = modelId,
            modelVersion = modelVersion
        )
        val now = System.currentTimeMillis()
        val revision = TranscriptRevisionEntity(
            id = revisionId,
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
            modelId = modelId,
            modelVersion = modelVersion,
            languageTag = languageTag,
            status = "COMPLETE",
            segmentCount = normalized.size,
            processingDurationMs = processingDurationMs,
            memoryObservationKb = memoryObservationKb,
            createdAt = now,
            updatedAt = now
        )
        val entities = normalized.mapIndexed { index, segment ->
            TranscriptSegmentEntity(
                id = stableSegmentId(revisionId, index, segment.startMs, segment.endMs, segment.rawText),
                revisionId = revisionId,
                recordingId = recordingId,
                orderIndex = index,
                startMs = segment.startMs,
                endMs = segment.endMs,
                rawText = segment.rawText,
                normalizedText = segment.text,
                speakerLabel = segment.speakerLabel
            )
        }
        transcriptDao.replaceRevisionSegments(revision, entities)
        return revision.toModel(entities)
    }

    private fun normalizeSegments(segments: List<TranscriptSegment>): List<TranscriptSegment> {
        val sorted = segments.sortedWith(compareBy({ it.startMs }, { it.endMs }))
        var previousEnd = 0L
        return sorted.mapIndexedNotNull { _, segment ->
            val start = segment.startMs.coerceAtLeast(previousEnd)
            val end = segment.endMs.coerceAtLeast(start)
            previousEnd = end
            val raw = segment.rawText.ifBlank { segment.text }
            val normalizedText = raw.trim().replace(Regex("\\s+"), " ")
            if (normalizedText.isBlank() || end <= start) {
                null
            } else {
                segment.copy(
                    id = "",
                    revisionId = "",
                    startMs = start,
                    endMs = end,
                    rawText = raw,
                    text = normalizedText
                )
            }
        }
    }

    private fun TranscriptRevisionEntity.toModel(segments: List<TranscriptSegmentEntity>): TranscriptRevision {
        return TranscriptRevision(
            id = id,
            recordingId = recordingId,
            sourceFingerprint = sourceFingerprint,
            pipelineVersion = pipelineVersion,
            modelId = modelId,
            modelVersion = modelVersion,
            languageTag = languageTag,
            createdAt = createdAt,
            segments = segments.sortedBy { it.orderIndex }.map { it.toModel() }
        )
    }

    private fun TranscriptSegmentEntity.toModel(): TranscriptSegment {
        return TranscriptSegment(
            id = id,
            revisionId = revisionId,
            startMs = startMs,
            endMs = endMs,
            rawText = rawText,
            text = normalizedText,
            speakerLabel = speakerLabel
        )
    }

    private fun stableRevisionId(
        recordingId: String,
        sourceFingerprint: String,
        modelId: String,
        modelVersion: String
    ): String = sha256(
        listOf(
            recordingId,
            sourceFingerprint,
            OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
            modelId,
            modelVersion
        ).joinToString("|")
    ).take(32)

    private fun stableSegmentId(
        revisionId: String,
        orderIndex: Int,
        startMs: Long,
        endMs: Long,
        rawText: String
    ): String = sha256("$revisionId|$orderIndex|$startMs|$endMs|$rawText").take(32)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(value.toByteArray())
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}
