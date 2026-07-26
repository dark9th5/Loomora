package com.loomora.core.model

data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val speakerLabel: String? = null
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
    data object ConsentRequired : AiJobStatus
    data object Uploading : AiJobStatus
    data object Transcribing : AiJobStatus
    data object Summarizing : AiJobStatus
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
