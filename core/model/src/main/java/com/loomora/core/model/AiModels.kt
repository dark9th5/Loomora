package com.loomora.core.model

data class TranscriptSegment(
    val id: String = "",
    val revisionId: String = "",
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val rawText: String = text,
    val speakerLabel: String? = null
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

data class ActionItem(
    val task: String,
    val assignee: String? = null,
    val dueDate: String? = null
)

data class Chapter(
    val title: String,
    val startMs: Long,
    val endMs: Long
)

data class AiInsights(
    val suggestedTitle: String,
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val decisions: List<String> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val chapters: List<Chapter> = emptyList()
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
    data class Failed(
        val message: String,
        val isRetryable: Boolean,
        val preservedTranscript: List<TranscriptSegment>? = null
    ) : AiJobStatus
}
