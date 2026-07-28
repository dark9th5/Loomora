package com.loomora.core.offlineai

import com.loomora.core.model.ActionItem
import com.loomora.core.model.AiInsights
import com.loomora.core.model.Chapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingInsightJsonParser @Inject constructor(
    private val json: Json
) {
    fun parse(rawJson: String, validSegmentIds: Set<String>): AiInsights {
        val decoded = runCatching {
            json.decodeFromString(InsightPayload.serializer(), rawJson.trim())
        }.getOrElse { throw OfflineAiException.InsightParseFailed }
        val allEvidence = mutableListOf<String>()
        fun requireEvidence(ids: List<String>, allowEmpty: Boolean = false) {
            if (!allowEmpty && ids.isEmpty()) throw OfflineAiException.InsightParseFailed
            if (ids.any { it !in validSegmentIds }) throw OfflineAiException.InsightParseFailed
            allEvidence += ids
        }

        requireEvidence(decoded.evidenceSegmentIds, allowEmpty = validSegmentIds.isEmpty())
        decoded.keyPoints.forEach { requireEvidence(it.evidenceSegmentIds) }
        decoded.decisions.forEach { requireEvidence(it.evidenceSegmentIds) }
        decoded.suggestions.forEach { requireEvidence(it.evidenceSegmentIds) }
        decoded.openQuestions.forEach { requireEvidence(it.evidenceSegmentIds) }
        decoded.actionItems.forEach { requireEvidence(it.evidenceSegmentIds) }
        decoded.chapters.forEach { requireEvidence(it.evidenceSegmentIds) }
        if (decoded.title.isBlank() || decoded.summary.isBlank()) {
            throw OfflineAiException.InsightParseFailed
        }
        return AiInsights(
            suggestedTitle = decoded.title.trim(),
            summary = decoded.summary.trim(),
            keyPoints = decoded.keyPoints.map { it.text.trim() }.filter { it.isNotBlank() }.distinct(),
            decisions = decoded.decisions.map { it.text.trim() }.filter { it.isNotBlank() }.distinct(),
            actionItems = decoded.actionItems.map {
                ActionItem(
                    task = it.task.trim(),
                    assignee = it.assignee?.trim()?.takeIf(String::isNotBlank),
                    dueDate = it.dueDate?.trim()?.takeIf(String::isNotBlank),
                    evidenceSegmentIds = it.evidenceSegmentIds.distinct()
                )
            }.filter { it.task.isNotBlank() }.distinctBy { listOf(it.task, it.assignee, it.dueDate).joinToString("|") },
            openQuestions = decoded.openQuestions.map { it.text.trim() }.filter { it.isNotBlank() }.distinct(),
            suggestions = decoded.suggestions.map { it.text.trim() }.filter { it.isNotBlank() }.distinct(),
            chapters = decoded.chapters.map {
                Chapter(
                    title = it.title.trim(),
                    startMs = it.startMs,
                    endMs = it.endMs.coerceAtLeast(it.startMs),
                    evidenceSegmentIds = it.evidenceSegmentIds.distinct()
                )
            }.filter { it.title.isNotBlank() },
            evidenceSegmentIds = (decoded.evidenceSegmentIds + allEvidence).distinct()
        )
    }
}

@Serializable
data class InsightPayload(
    @SerialName("smartTitle") val title: String,
    val summary: String,
    val keyPoints: List<TextEvidence> = emptyList(),
    val decisions: List<TextEvidence> = emptyList(),
    val suggestions: List<TextEvidence> = emptyList(),
    val openQuestions: List<TextEvidence> = emptyList(),
    val actionItems: List<ActionEvidence> = emptyList(),
    val chapters: List<ChapterEvidence> = emptyList(),
    val evidenceSegmentIds: List<String> = emptyList()
)

@Serializable
data class TextEvidence(
    val text: String,
    val evidenceSegmentIds: List<String> = emptyList()
)

@Serializable
data class ActionEvidence(
    val task: String,
    val assignee: String? = null,
    val dueDate: String? = null,
    val evidenceSegmentIds: List<String> = emptyList()
)

@Serializable
data class ChapterEvidence(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val evidenceSegmentIds: List<String> = emptyList()
)
