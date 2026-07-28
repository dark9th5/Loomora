package com.loomora.core.offlineai

import com.loomora.core.model.ActionItem
import com.loomora.core.model.AiInsights
import com.loomora.core.model.Chapter
import com.loomora.core.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class HeuristicMeetingInsightEngine @Inject constructor(
    private val json: Json
) : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput = withContext(Dispatchers.Default) {
        val startedAt = System.currentTimeMillis()
        val segments = input.transcriptRevision.segments
            .filter { it.text.isNotBlank() }
            .sortedBy { it.startMs }

        if (segments.isEmpty()) {
            return@withContext MeetingInsightOutput(
                insights = AiInsights(
                    suggestedTitle = if (input.languageTag == "vi") "Ban ghi khong co noi dung" else "Empty meeting",
                    summary = if (input.languageTag == "vi") "Khong co transcript de tao insight." else "No transcript text was available for local insights."
                ),
                modelId = OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID,
                modelVersion = OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_VERSION,
                promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
                schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
                pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
                languageTag = input.languageTag,
                chunkCheckpoints = emptyList(),
                modelSizeBytes = 0L,
                loadTimeMs = 0L,
                generationTimeMs = System.currentTimeMillis() - startedAt,
                memoryObservationKb = currentUsedMemoryKb()
            )
        }

        coroutineContext.ensureActive()
        val ranked = segments.sortedWith(
            compareByDescending<TranscriptSegment> { score(it.text) }
                .thenBy { it.startMs }
        )
        val evidenceIds = ranked.take(4).map { it.id }.filter { it.isNotBlank() }.ifEmpty {
            segments.take(1).map { it.id }
        }
        val decisions = segments.extractMatches(decisionRegex)
        val questions = segments
            .filter { questionRegex.containsMatchIn(it.text) || it.text.trim().endsWith("?") }
            .take(3)
            .map { cleanSentence(it.text) }
            .distinct()
        val suggestions = segments.extractMatches(suggestionRegex)
        val actions = segments
            .filter { actionRegex.containsMatchIn(it.text) }
            .take(3)
            .map {
                ActionItem(
                    task = cleanSentence(it.text),
                    assignee = explicitAssignee(it.text),
                    dueDate = explicitDueDate(it.text),
                    evidenceSegmentIds = listOf(it.id)
                )
            }
            .distinctBy { it.task.lowercase() }
        val keyPoints = ranked
            .take(4)
            .map { cleanSentence(it.text) }
            .filter { it.isNotBlank() }
            .distinct()
        val insights = AiInsights(
            suggestedTitle = titleFrom(segments, decisions, input.languageTag),
            summary = keyPoints.take(2).joinToString(" ").ifBlank { cleanSentence(segments.first().text) },
            keyPoints = keyPoints,
            decisions = decisions,
            actionItems = actions,
            openQuestions = questions,
            suggestions = suggestions,
            chapters = buildChapters(segments),
            evidenceSegmentIds = evidenceIds.distinct()
        )

        MeetingInsightOutput(
            insights = insights,
            modelId = OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID,
            modelVersion = OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_VERSION,
            promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
            schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
            pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
            languageTag = input.languageTag,
            chunkCheckpoints = listOf(
                InsightChunkCheckpoint(
                    chunkIndex = 0,
                    startMs = segments.first().startMs,
                    endMs = segments.last().endMs,
                    segmentIds = segments.map { it.id },
                    outputJson = json.encodeToString(insights)
                )
            ),
            modelSizeBytes = 0L,
            loadTimeMs = 0L,
            generationTimeMs = System.currentTimeMillis() - startedAt,
            memoryObservationKb = currentUsedMemoryKb()
        )
    }

    override fun close() = Unit

    private fun List<TranscriptSegment>.extractMatches(regex: Regex): List<String> {
        return filter { regex.containsMatchIn(it.text) }
            .take(3)
            .map { cleanSentence(it.text) }
            .distinct()
    }

    private fun titleFrom(
        segments: List<TranscriptSegment>,
        decisions: List<String>,
        languageTag: String?
    ): String {
        val source = decisions.firstOrNull() ?: segments.maxByOrNull { score(it.text) }?.text.orEmpty()
        val words = source
            .replace(Regex("[^\\p{L}\\p{N}\\s-]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it.lowercase() !in stopWords }
            .take(6)
        return words.joinToString(" ").ifBlank {
            if (languageTag == "vi") "Tom tat cuoc hop" else "Meeting insights"
        }
    }

    private fun buildChapters(segments: List<TranscriptSegment>): List<Chapter> {
        val first = segments.first()
        val last = segments.last()
        if (segments.size <= 3) {
            return listOf(
                Chapter(
                    title = cleanSentence(first.text).take(48).ifBlank { "Transcript" },
                    startMs = first.startMs,
                    endMs = last.endMs,
                    evidenceSegmentIds = segments.map { it.id }.take(3)
                )
            )
        }
        val midpoint = segments.size / 2
        return listOf(
            chapterFrom("Opening", segments.take(midpoint)),
            chapterFrom("Follow up", segments.drop(midpoint))
        )
    }

    private fun chapterFrom(title: String, segments: List<TranscriptSegment>): Chapter {
        return Chapter(
            title = title,
            startMs = segments.first().startMs,
            endMs = segments.last().endMs,
            evidenceSegmentIds = segments.map { it.id }.take(3)
        )
    }

    private fun score(text: String): Int {
        val lower = text.lowercase()
        return lower.length / 30 +
            keywordWeights.sumOf { (keyword, weight) -> if (lower.contains(keyword)) weight else 0 }
    }

    private fun cleanSentence(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim().trim('.', ',', ';', ':')
    }

    private fun explicitAssignee(text: String): String? {
        val match = Regex("\\b(?:ask|asks|assigned to|giao cho|nh\\u1edd)\\s+([\\p{L}][\\p{L}\\p{N}_-]{1,24})", RegexOption.IGNORE_CASE)
            .find(text)
        return match?.groupValues?.getOrNull(1)
    }

    private fun explicitDueDate(text: String): String? {
        val match = Regex("\\b(?:today|tomorrow|friday|monday|tuesday|wednesday|thursday|saturday|sunday|h\\u00f4m nay|ng\\u00e0y mai|th\\u1ee9 hai|th\\u1ee9 ba|th\\u1ee9 t\\u01b0|th\\u1ee9 n\\u0103m|th\\u1ee9 s\\u00e1u|th\\u1ee9 b\\u1ea3y|ch\\u1ee7 nh\\u1eadt)\\b", RegexOption.IGNORE_CASE)
            .find(text)
        return match?.value
    }

    private fun currentUsedMemoryKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L
    }

    private companion object {
        val decisionRegex = Regex("\\b(decide|decides|decided|decision|ch\\u1ed1t|chot|quy\\u1ebft \\u0111\\u1ecbnh|quyet dinh|th\\u1ed1ng nh\\u1ea5t|thong nhat)\\b", RegexOption.IGNORE_CASE)
        val questionRegex = Regex("\\b(question|asks?|whether|open question|c\\u00e2u h\\u1ecfi|cau hoi|h\\u1ecfi|hoi|li\\u1ec7u|lieu)\\b", RegexOption.IGNORE_CASE)
        val suggestionRegex = Regex("\\b(should|suggest|recommend|c\\u1ea7n|can|n\\u00ean|nen|\\u0111\\u1ec1 xu\\u1ea5t|de xuat|g\\u1ee3i \\u00fd|goi y)\\b", RegexOption.IGNORE_CASE)
        val actionRegex = Regex("\\b(action|todo|follow up|review|send|publish|assign|asks?|c\\u1ea7n|can|ph\\u1ea3i|phai|g\\u1eedi|gui|xem l\\u1ea1i|xem lai|tri\\u1ec3n khai|trien khai|giao)\\b", RegexOption.IGNORE_CASE)
        val keywordWeights = listOf(
            "decide" to 5,
            "decision" to 5,
            "ch\\u1ed1t" to 5,
            "quy\\u1ebft \\u0111\\u1ecbnh" to 5,
            "chot" to 5,
            "quyet dinh" to 5,
            "thong nhat" to 5,
            "action" to 4,
            "todo" to 4,
            "follow up" to 4,
            "can" to 4,
            "phai" to 4,
            "deadline" to 3,
            "release" to 3,
            "beta" to 3,
            "question" to 3,
            "c\\u00e2u h\\u1ecfi" to 3,
            "cau hoi" to 3
        )
        val stopWords = setOf(
            "the", "and", "for", "that", "this", "with", "from", "says", "asks",
            "m\\u1ed9t", "v\\u00e0", "cho", "c\\u1ee7a", "l\\u00e0", "c\\u1ea7n", "n\\u00ean", "v\\u1edbi"
        )
    }
}

typealias ExtractiveMeetingInsightEngine = HeuristicMeetingInsightEngine
