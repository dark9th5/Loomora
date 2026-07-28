package com.loomora.core.model

import kotlinx.serialization.Serializable

data class TranscriptSegment(
    val id: String = "",
    val revisionId: String = "",
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val rawText: String = text,
    val speakerLabel: String? = null,
    val speakerConfidence: Float? = null,
    val speakerIsUncertain: Boolean = false
)

data class TranscriptRevision(
    val id: String,
    val recordingId: String,
    val sourceFingerprint: String,
    val pipelineVersion: String,
    val modelId: String,
    val modelVersion: String,
    val languageTag: String?,
    val createdAt: Long,
    val segments: List<TranscriptSegment>
)

data class SpeakerTurn(
    val id: String = "",
    val revisionId: String = "",
    val recordingId: String = "",
    val startMs: Long,
    val endMs: Long,
    val speakerLabel: String,
    val speakerIndex: Int,
    val confidence: Float? = null,
    val isOverlapped: Boolean = false,
    val isUncertain: Boolean = false,
    val alternateSpeakerLabels: List<String> = emptyList()
)

data class SpeakerAlias(
    val recordingId: String,
    val genericLabel: String,
    val displayName: String,
    val updatedAt: Long
)

data class DiarizationRevision(
    val id: String,
    val recordingId: String,
    val sourceFingerprint: String,
    val pipelineVersion: String,
    val modelId: String,
    val modelVersion: String,
    val clusteringSettings: String,
    val createdAt: Long,
    val turns: List<SpeakerTurn>
)

@Serializable
data class ActionItem(
    val task: String,
    val assignee: String? = null,
    val dueDate: String? = null,
    val evidenceSegmentIds: List<String> = emptyList()
)

@Serializable
data class Chapter(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val evidenceSegmentIds: List<String> = emptyList()
)

@Serializable
data class AiInsights(
    val suggestedTitle: String,
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val decisions: List<String> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val openQuestions: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val evidenceSegmentIds: List<String> = emptyList()
)

enum class InsightRevisionKind {
    GENERATED,
    USER_EDITED
}

data class InsightRevision(
    val id: String,
    val recordingId: String,
    val transcriptRevisionId: String,
    val sourceFingerprint: String,
    val pipelineVersion: String,
    val promptVersion: String,
    val schemaVersion: String,
    val modelId: String,
    val modelVersion: String,
    val languageTag: String?,
    val kind: InsightRevisionKind,
    val createdAt: Long,
    val insights: AiInsights,
    val modelSizeBytes: Long,
    val loadTimeMs: Long,
    val generationTimeMs: Long,
    val memoryObservationKb: Long?,
    val generationMode: String = "HEURISTIC",
    val completionQuality: String = "EXTRACTIVE_ONLY",
    val fallbackReason: String? = null
)

sealed interface AiJobStatus {
    data object Idle : AiJobStatus
    data object VerifyingModels : AiJobStatus
    data object PreparingAudio : AiJobStatus
    data class Queued(val jobId: String) : AiJobStatus
    data class Processing(val stage: String, val progress: Float) : AiJobStatus
    data class Partial(val transcript: List<TranscriptSegment>, val progress: Float) : AiJobStatus
    data object Cancelled : AiJobStatus
    data class ModelRequired(val requiredCapabilities: List<String>) : AiJobStatus
    data class Completed(
        val transcript: List<TranscriptSegment>,
        val insights: AiInsights?
    ) : AiJobStatus
    data class CompletedWithHeuristicFallback(
        val transcript: List<TranscriptSegment>,
        val insights: AiInsights,
        val reason: String
    ) : AiJobStatus
    data class Failed(
        val message: String,
        val isRetryable: Boolean,
        val preservedTranscript: List<TranscriptSegment>? = null
    ) : AiJobStatus
}
