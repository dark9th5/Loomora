package com.loomora.core.offlineai

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
import kotlin.math.sqrt

@Singleton
class HeuristicMeetingInsightEngine @Inject constructor(
    private val json: Json
) : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput = withContext(Dispatchers.Default) {
        val startedAt = System.currentTimeMillis()
        val segments = TranscriptSpeakerFusion.compact(
            input.transcriptRevision.segments
                .filter { it.text.isNotBlank() }
                .sortedBy { it.startMs }
        )
        val vietnamese = input.languageTag?.startsWith("vi", ignoreCase = true) == true
        if (segments.isEmpty()) {
            return@withContext outputFor(
                input = input,
                insights = AiInsights(
                    suggestedTitle = if (vietnamese) "Bản ghi không có nội dung" else "Empty recording",
                    summary = if (vietnamese) "Không có bản chép lời để phân tích." else "No transcript text was available for analysis."
                ),
                segments = emptyList(),
                startedAt = startedAt
            )
        }

        coroutineContext.ensureActive()
        val candidates = buildCandidates(segments)
        val corpusTerms = candidates.flatMap { it.terms }
        val frequencies = corpusTerms.groupingBy { it }.eachCount()
        val topics = extractTopicPhrases(segments, limit = 4)
        val phasePoints = buildPhasePoints(segments, vietnamese)

        val scored = candidates.mapIndexed { index, candidate ->
            candidate.copy(score = score(candidate, frequencies, index, candidates.size))
        }
        val decisionCandidates = selectDiverse(scored.filter { decisionRegex.containsMatchIn(it.text) }, 3)
        val actionCandidates = selectDiverse(scored.filter { HeuristicActionItemExtractor.isActionable(it.text) }, 5)
        val questionCandidates = selectDiverse(scored.filter { questionRegex.containsMatchIn(it.text) || it.text.trim().endsWith("?") }, 3)
        val suggestionCandidates = selectDiverse(scored.filter { suggestionRegex.containsMatchIn(it.text) }, 3)

        val keyPoints = phasePoints.map { it.text }
        val decisions = decisionCandidates.map { cleanSentence(it.text) }
        val questions = questionCandidates.map { cleanSentence(it.text) }
        val suggestions = suggestionCandidates.map { cleanSentence(it.text) }
        val actions = actionCandidates.mapNotNull { candidate ->
            HeuristicActionItemExtractor.extract(candidate.text, candidate.segmentIds)
        }.distinctBy { normalizeForComparison(it.task) }

        val insights = AiInsights(
            suggestedTitle = synthesizeTitle(topics, vietnamese),
            summary = synthesizeSummary(
                topics = topics,
                keyPoints = keyPoints,
                decisions = decisions,
                actions = actions.map { it.task },
                vietnamese = vietnamese
            ),
            keyPoints = keyPoints,
            decisions = decisions,
            actionItems = actions,
            openQuestions = questions,
            suggestions = suggestions,
            chapters = buildTopicChapters(segments, vietnamese),
            evidenceSegmentIds = (
                phasePoints.flatMap { it.segmentIds } +
                    (decisionCandidates + actionCandidates + questionCandidates).flatMap { it.segmentIds }
                )
                .filter(String::isNotBlank)
                .distinct()
        )
        outputFor(input, insights, segments, startedAt)
    }

    override fun close() = Unit

    private fun buildCandidates(segments: List<TranscriptSegment>): List<Candidate> = buildList {
        segments.forEach { segment ->
            val sentences = segment.text
                .replace(Regex("\\s+"), " ")
                .split(Regex("(?<=[.!?…])\\s+"))
                .map(::cleanSentence)
                .filter { it.wordCount() >= 4 }
                .ifEmpty { listOf(cleanSentence(segment.text)) }
            sentences.forEach { sentence ->
                add(
                    Candidate(
                        text = sentence,
                        startMs = segment.startMs,
                        endMs = segment.endMs,
                        speakerLabel = segment.speakerLabel,
                        segmentIds = listOf(segment.id).filter(String::isNotBlank),
                        terms = tokenize(sentence)
                    )
                )
            }
        }
    }

    private fun score(
        candidate: Candidate,
        frequencies: Map<String, Int>,
        index: Int,
        total: Int
    ): Double {
        val termScore = candidate.terms.sumOf { frequencies[it]?.toDouble() ?: 0.0 } /
            sqrt(candidate.terms.size.coerceAtLeast(1).toDouble())
        val positionBoost = when {
            index == 0 -> 1.4
            index == total - 1 -> 1.2
            else -> 1.0
        }
        val semanticBoost = listOf(
            decisionRegex to 3.0,
            actionRegex to 2.4,
            questionRegex to 1.6,
            suggestionRegex to 1.4
        ).sumOf { (regex, boost) -> if (regex.containsMatchIn(candidate.text)) boost else 0.0 }
        return termScore * positionBoost + semanticBoost
    }

    private fun selectDiverse(candidates: List<Candidate>, limit: Int): List<Candidate> {
        val selected = mutableListOf<Candidate>()
        candidates.sortedByDescending { it.score }.forEach { candidate ->
            if (selected.size >= limit) return@forEach
            val duplicate = selected.any { similarity(it.terms, candidate.terms) >= 0.58 }
            if (!duplicate && candidate.text.isNotBlank()) selected += candidate
        }
        return selected.sortedBy { it.startMs }
    }

    private fun similarity(a: List<String>, b: List<String>): Double {
        val left = a.toSet()
        val right = b.toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return left.intersect(right).size.toDouble() / left.union(right).size.toDouble()
    }

    private fun extractTopicPhrases(segments: List<TranscriptSegment>, limit: Int): List<String> {
        val tokens = segments.flatMap { tokenize(it.text) }
        if (tokens.isEmpty()) return emptyList()

        var phraseIndex = 0
        val bigrams = segments.flatMap { segment ->
            rawTokens(segment.text)
                .windowed(size = 2, step = 1, partialWindows = false)
                .filter { words -> words.all { it.length >= 3 && it !in stopWords } }
                .map { words -> TopicPhrase(words.joinToString(" "), words.toSet(), phraseIndex++) }
        }
        val counts = bigrams.groupingBy { it.text }.eachCount()
        val ranked = bigrams
            .distinctBy { it.text }
            .sortedWith(
                compareByDescending<TopicPhrase> { counts[it.text] ?: 0 }
                    .thenBy { it.firstIndex }
            )

        val selected = mutableListOf<TopicPhrase>()
        ranked.forEach { phrase ->
            if (selected.size >= limit) return@forEach
            val overlaps = selected.any { existing ->
                phrase.terms.intersect(existing.terms).isNotEmpty()
            }
            if (!overlaps) selected += phrase
        }

        if (selected.isNotEmpty()) return selected.map { it.text }
        return tokens.distinct().take(limit)
    }

    private fun buildPhasePoints(
        segments: List<TranscriptSegment>,
        vietnamese: Boolean
    ): List<SynthesizedPoint> {
        val groupCount = when {
            segments.size < 3 -> 1
            segments.size < 8 -> 2
            else -> 3
        }
        return List(groupCount) { index ->
            val from = index * segments.size / groupCount
            val to = ((index + 1) * segments.size / groupCount).coerceAtLeast(from + 1)
            val group = segments.subList(from, minOf(to, segments.size))
            val topics = extractTopicPhrases(group, limit = 3)
            val topicText = topics.joinToString(", ").ifBlank {
                if (vietnamese) "nội dung trao đổi chính" else "the main discussion"
            }
            val text = if (vietnamese) {
                when {
                    groupCount == 1 -> "Toàn bộ cuộc trao đổi tập trung vào $topicText."
                    index == 0 -> "Phần mở đầu tập trung vào $topicText."
                    index == groupCount - 1 -> "Phần cuối chuyển sang $topicText."
                    else -> "Ở phần giữa, nội dung phát triển sang $topicText."
                }
            } else {
                when {
                    groupCount == 1 -> "The discussion as a whole focused on $topicText."
                    index == 0 -> "The opening focused on $topicText."
                    index == groupCount - 1 -> "The closing moved to $topicText."
                    else -> "The middle of the discussion developed into $topicText."
                }
            }
            SynthesizedPoint(
                text = text,
                segmentIds = group.map { it.id }.filter(String::isNotBlank)
            )
        }
    }

    private fun synthesizeTitle(topics: List<String>, vietnamese: Boolean): String {
        val focus = topics.take(3).joinToString(if (vietnamese) ", " else ", ")
        return when {
            focus.isBlank() && vietnamese -> "Tóm tắt cuộc trao đổi"
            focus.isBlank() -> "Meeting overview"
            vietnamese -> "Trao đổi về $focus"
            else -> "Discussion: $focus"
        }
    }

    private fun synthesizeSummary(
        topics: List<String>,
        keyPoints: List<String>,
        decisions: List<String>,
        actions: List<String>,
        vietnamese: Boolean
    ): String {
        val topicText = topics.take(4).joinToString(", ")
        val mainIdeas = keyPoints.take(3).joinToString("; ")
        val conciseDecision = decisions.firstOrNull { it.wordCount() <= 28 }
        val conciseAction = actions.firstOrNull { it.wordCount() <= 28 }
        return if (vietnamese) {
            buildList {
                if (topicText.isNotBlank()) add("Cuộc trao đổi tập trung vào $topicText.")
                if (keyPoints.size > 1 && mainIdeas.isNotBlank()) add("Diễn biến chính: $mainIdeas")
                conciseDecision?.let { add("Kết luận hoặc thống nhất nổi bật: ${cleanSentence(it)}.") }
                conciseAction?.let { add("Việc tiếp theo được nêu ra: ${cleanSentence(it)}.") }
                if (isEmpty()) addAll(keyPoints)
            }.joinToString(" ")
        } else {
            buildList {
                if (topicText.isNotBlank()) add("The discussion focused on $topicText.")
                if (keyPoints.size > 1 && mainIdeas.isNotBlank()) add("The discussion developed as follows: $mainIdeas")
                conciseDecision?.let { add("The clearest decision was: ${cleanSentence(it)}.") }
                conciseAction?.let { add("The next action mentioned was: ${cleanSentence(it)}.") }
                if (isEmpty()) addAll(keyPoints)
            }.joinToString(" ")
        }
    }

    private fun buildTopicChapters(segments: List<TranscriptSegment>, vietnamese: Boolean): List<Chapter> {
        if (segments.isEmpty()) return emptyList()
        val chapterCount = when {
            segments.size < 4 -> 1
            segments.size < 10 -> 2
            else -> 3
        }
        return List(chapterCount) { index ->
            val from = index * segments.size / chapterCount
            val to = (index + 1) * segments.size / chapterCount
            val group = segments.subList(from, to.coerceAtLeast(from + 1))
            val localTopics = extractTopicPhrases(group, limit = 3)
            val fallback = if (vietnamese) "Chủ đề ${index + 1}" else "Topic ${index + 1}"
            Chapter(
                title = localTopics.joinToString(", ").ifBlank { fallback },
                startMs = group.first().startMs,
                endMs = group.last().endMs,
                evidenceSegmentIds = group.map { it.id }.filter(String::isNotBlank).take(5)
            )
        }
    }

    private fun outputFor(
        input: MeetingInsightInput,
        insights: AiInsights,
        segments: List<TranscriptSegment>,
        startedAt: Long
    ): MeetingInsightOutput = MeetingInsightOutput(
        insights = insights,
        modelId = OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID,
        modelVersion = OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_VERSION,
        promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
        schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
        pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
        languageTag = input.languageTag,
        chunkCheckpoints = if (segments.isEmpty()) emptyList() else listOf(
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
        memoryObservationKb = currentUsedMemoryKb(),
        generationMode = InsightGenerationMode.HEURISTIC,
        completionQuality = InsightCompletionQuality.EXTRACTIVE_ONLY
    )

    private fun rawTokens(text: String): List<String> = text.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)

    private fun tokenize(text: String): List<String> = text.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length >= 3 && it !in stopWords }

    private fun cleanSentence(text: String): String = text.replace(Regex("\\s+"), " ").trim().trim('.', ',', ';', ':')
    private fun normalizeForComparison(text: String): String = tokenize(text).joinToString(" ")
    private fun String.wordCount(): Int = split(Regex("\\s+")).count(String::isNotBlank)

    private fun currentUsedMemoryKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L
    }

    private data class Candidate(
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val speakerLabel: String?,
        val segmentIds: List<String>,
        val terms: List<String>,
        val score: Double = 0.0
    )

    private data class SynthesizedPoint(
        val text: String,
        val segmentIds: List<String>
    )

    private data class TopicPhrase(
        val text: String,
        val terms: Set<String>,
        val firstIndex: Int
    )

    private companion object {
        val decisionRegex = Regex("\\b(decide|decides|decided|decision|chốt|quyết định|thống nhất|đồng ý)\\b", RegexOption.IGNORE_CASE)
        val questionRegex = Regex("\\b(question|asks?|whether|open question|câu hỏi|hỏi|liệu|chưa rõ)\\b", RegexOption.IGNORE_CASE)
        val suggestionRegex = Regex("\\b(should|suggest|recommend|cần|nên|đề xuất|gợi ý|khuyến nghị)\\b", RegexOption.IGNORE_CASE)
        val actionRegex = Regex("\\b(action|todo|follow up|review|send|publish|assign|cần|phải|gửi|xem lại|triển khai|giao|hoàn thành)\\b", RegexOption.IGNORE_CASE)
        val stopWords = setOf(
            "the", "and", "for", "that", "this", "with", "from", "have", "has", "was", "were", "will", "would", "about",
            "một", "và", "cho", "của", "là", "với", "những", "các", "được", "trong", "đang", "này", "đó", "thì", "như", "khi", "tôi", "bạn", "chúng", "mình",
            "hôm", "nay", "sau", "trước", "toàn", "bộ", "cũng", "rất", "để", "về", "trên", "dưới", "sẽ", "cần", "phải", "nên", "lại", "ta", "phần"
        )
    }
}

typealias ExtractiveMeetingInsightEngine = HeuristicMeetingInsightEngine
