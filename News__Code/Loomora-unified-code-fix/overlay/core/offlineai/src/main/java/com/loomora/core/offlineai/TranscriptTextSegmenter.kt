package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment
import kotlin.math.abs

/**
 * Turns one ASR result into readable transcript rows without losing text.
 *
 * ASR engines often return a very long block when a speaker talks quickly and
 * does not pause. Rows are therefore bounded by word count even when the model
 * produced no punctuation. Sentence and clause punctuation are preferred as
 * break points; a balanced word boundary is used only as a fallback.
 */
internal object TranscriptTextSegmenter {
    private const val TARGET_WORDS_PER_SEGMENT = 28
    private const val MIN_WORDS_PER_SEGMENT = 8
    private const val MAX_WORDS_PER_SEGMENT = 38
    private const val MIN_WORDS_PER_INTERVAL = 7

    private val sentenceBoundary = Regex("(?<=[.!?…;])\\s+|[\\r\\n]+")
    private val strongBreak = Regex("[.!?…;][\\\"'”’)]*$")
    private val softBreak = Regex("[,：:][\\\"'”’)]*$")
    private val connectorWords = setOf(
        "and", "but", "however", "so", "then", "therefore", "because", "also", "meanwhile",
        "và", "nhưng", "tuy", "nên", "vì", "rồi", "sau", "đồng", "ngoài", "tiếp"
    )

    fun segment(text: String, startMs: Long, endMs: Long): List<TranscriptSegment> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        val sentenceParts = normalized
            .split(sentenceBoundary)
            .map(::normalize)
            .filter(String::isNotBlank)
        val chunks = coalesceSentences(sentenceParts.ifEmpty { listOf(normalized) })
        if (chunks.size == 1) {
            return listOf(newSegment(chunks.single(), text, startMs, endMs))
        }

        return projectChunksOntoTime(chunks, startMs, endMs)
    }

    /**
     * Splits an already persisted legacy row for display only. Speaker metadata
     * is preserved while timestamps are projected proportionally onto each row.
     */
    fun splitSegment(segment: TranscriptSegment): List<TranscriptSegment> {
        val pieces = segment(segment.text, segment.startMs, segment.endMs)
        if (pieces.isEmpty()) return emptyList()
        if (pieces.size == 1) {
            return listOf(
                segment.copy(
                    text = normalize(segment.text),
                    rawText = normalize(segment.rawText.ifBlank { segment.text })
                )
            )
        }
        return pieces.map { piece ->
            segment.copy(
                startMs = piece.startMs,
                endMs = piece.endMs,
                text = piece.text,
                rawText = piece.text
            )
        }
    }

    /**
     * Used only when one ASR block must be projected onto reliable speaker turns.
     * The number of pieces is capped by the amount of text, so a 12-word sentence
     * can no longer be split into four 3-word transcript rows.
     */
    fun splitAcrossIntervals(text: String, intervalCount: Int): List<String> {
        val words = normalize(text).words()
        if (words.isEmpty()) return emptyList()
        if (intervalCount <= 1) return listOf(words.joinToString(" "))

        val maxUsefulIntervals = (words.size / MIN_WORDS_PER_INTERVAL).coerceAtLeast(1)
        val actualCount = minOf(intervalCount, maxUsefulIntervals)
        if (actualCount <= 1) return listOf(words.joinToString(" "))

        return List(actualCount) { index ->
            val from = index * words.size / actualCount
            val to = (index + 1) * words.size / actualCount
            words.subList(from, to).joinToString(" ")
        }.mergeTinyTail(MIN_WORDS_PER_INTERVAL)
    }

    private fun coalesceSentences(sentences: List<String>): List<String> {
        val units = sentences.flatMap(::splitLongUnit)
        val result = mutableListOf<String>()
        var current = mutableListOf<String>()
        var currentWords = 0

        fun flush() {
            val value = normalize(current.joinToString(" "))
            if (value.isNotBlank()) result += value
            current = mutableListOf()
            currentWords = 0
        }

        units.forEach { unit ->
            val words = unit.wordCount()
            when {
                currentWords == 0 -> {
                    current += unit
                    currentWords = words
                }
                currentWords + words <= TARGET_WORDS_PER_SEGMENT -> {
                    current += unit
                    currentWords += words
                }
                else -> {
                    flush()
                    current += unit
                    currentWords = words
                }
            }
        }
        if (currentWords > 0) flush()
        return result.rebalanceTinyTail()
    }

    private fun splitLongUnit(value: String): List<String> {
        val words = normalize(value).words()
        if (words.size <= MAX_WORDS_PER_SEGMENT) return listOf(words.joinToString(" "))

        val result = mutableListOf<String>()
        var from = 0
        while (words.size - from > MAX_WORDS_PER_SEGMENT) {
            val remaining = words.size - from
            val maxCut = minOf(from + MAX_WORDS_PER_SEGMENT, words.size - MIN_WORDS_PER_SEGMENT)
            val minCut = minOf(from + MIN_WORDS_PER_SEGMENT, maxCut)
            val targetCut = minOf(from + TARGET_WORDS_PER_SEGMENT, maxCut)
            val cut = findBestCut(words, minCut, maxCut, targetCut)
            result += words.subList(from, cut).joinToString(" ")
            from = cut

            // Defensive guard for malformed limits; should never be reached.
            if (remaining == words.size - from) break
        }
        if (from < words.size) result += words.subList(from, words.size).joinToString(" ")
        return result.rebalanceTinyTail()
    }

    private fun findBestCut(
        words: List<String>,
        minCut: Int,
        maxCut: Int,
        targetCut: Int
    ): Int {
        var bestCut = targetCut.coerceIn(minCut, maxCut)
        var bestScore = Int.MIN_VALUE
        for (cut in minCut..maxCut) {
            val previous = words[cut - 1]
            val next = words.getOrNull(cut)?.trimPunctuation()?.lowercase()
            val boundaryScore = when {
                strongBreak.containsMatchIn(previous) -> 120
                softBreak.containsMatchIn(previous) -> 80
                next in connectorWords -> 45
                else -> 0
            }
            val distancePenalty = abs(cut - targetCut) * 3
            val score = boundaryScore - distancePenalty
            if (score > bestScore) {
                bestScore = score
                bestCut = cut
            }
        }
        return bestCut
    }

    private fun List<String>.rebalanceTinyTail(): List<String> {
        if (size <= 1 || last().wordCount() >= MIN_WORDS_PER_SEGMENT) return this
        val mutable = toMutableList()
        val tailWords = mutable.last().words().toMutableList()
        val previousWords = mutable[mutable.lastIndex - 1].words().toMutableList()

        if (previousWords.size + tailWords.size <= MAX_WORDS_PER_SEGMENT) {
            mutable[mutable.lastIndex - 1] = (previousWords + tailWords).joinToString(" ")
            mutable.removeAt(mutable.lastIndex)
            return mutable
        }

        val needed = MIN_WORDS_PER_SEGMENT - tailWords.size
        val movable = (previousWords.size - MIN_WORDS_PER_SEGMENT).coerceAtLeast(0)
        val moveCount = minOf(needed, movable)
        if (moveCount > 0) {
            val moved = previousWords.takeLast(moveCount)
            repeat(moveCount) { previousWords.removeAt(previousWords.lastIndex) }
            mutable[mutable.lastIndex - 1] = previousWords.joinToString(" ")
            mutable[mutable.lastIndex] = (moved + tailWords).joinToString(" ")
        }
        return mutable
    }

    private fun List<String>.mergeTinyTail(minWords: Int): List<String> {
        if (size <= 1 || last().wordCount() >= minWords) return this
        return dropLast(2) + normalize(this[size - 2] + " " + last())
    }

    private fun projectChunksOntoTime(
        chunks: List<String>,
        startMs: Long,
        endMs: Long
    ): List<TranscriptSegment> {
        val safeEnd = endMs.coerceAtLeast(startMs + chunks.size)
        val totalWeight = chunks.sumOf(::weight).coerceAtLeast(1)
        var elapsedWeight = 0
        return chunks.mapIndexed { index, chunk ->
            val chunkStart = startMs + (safeEnd - startMs) * elapsedWeight / totalWeight
            elapsedWeight += weight(chunk)
            val chunkEnd = if (index == chunks.lastIndex) {
                safeEnd
            } else {
                startMs + (safeEnd - startMs) * elapsedWeight / totalWeight
            }
            newSegment(chunk, chunk, chunkStart, chunkEnd.coerceAtLeast(chunkStart + 1L))
        }
    }

    private fun String.words(): List<String> = split(Regex("\\s+")).filter(String::isNotBlank)
    private fun String.wordCount(): Int = words().size
    private fun String.trimPunctuation(): String = trim('"', '\'', '“', '”', '‘', '’', '(', ')', '[', ']', ',', '.', ':', ';', '!', '?', '…')
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    private fun weight(text: String): Int = text.count { !it.isWhitespace() }.coerceAtLeast(1)

    private fun newSegment(text: String, rawText: String, startMs: Long, endMs: Long) = TranscriptSegment(
        startMs = startMs,
        endMs = endMs.coerceAtLeast(startMs + 1L),
        rawText = rawText,
        text = text
    )
}
