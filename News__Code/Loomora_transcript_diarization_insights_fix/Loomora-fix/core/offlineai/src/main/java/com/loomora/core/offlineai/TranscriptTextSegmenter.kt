package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment

internal object TranscriptTextSegmenter {
    private const val TARGET_WORDS_PER_SEGMENT = 42
    private const val MIN_WORDS_PER_SEGMENT = 8
    private const val MAX_WORDS_PER_SEGMENT = 64
    private const val MIN_WORDS_PER_INTERVAL = 7

    fun segment(text: String, startMs: Long, endMs: Long): List<TranscriptSegment> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        val sentenceParts = normalized
            .split(Regex("(?<=[.!?…])\\s+"))
            .map(::normalize)
            .filter(String::isNotBlank)
        val chunks = coalesceSentences(sentenceParts.ifEmpty { listOf(normalized) })
        if (chunks.size == 1) {
            return listOf(newSegment(chunks.single(), text, startMs, endMs))
        }

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

    /**
     * Used only when one ASR block must be projected onto reliable speaker turns.
     * The number of pieces is capped by the amount of text, so a 12-word sentence
     * can no longer be split into four 3-word transcript rows.
     */
    fun splitAcrossIntervals(text: String, intervalCount: Int): List<String> {
        val words = normalize(text).split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        if (intervalCount <= 1) return listOf(words.joinToString(" "))

        val maxUsefulIntervals = (words.size / MIN_WORDS_PER_INTERVAL).coerceAtLeast(1)
        val actualCount = minOf(intervalCount, maxUsefulIntervals)
        if (actualCount <= 1) return listOf(words.joinToString(" "))

        return List(actualCount) { index ->
            val from = index * words.size / actualCount
            val to = (index + 1) * words.size / actualCount
            words.subList(from, to).joinToString(" ")
        }.mergeTinyTail()
    }

    private fun coalesceSentences(sentences: List<String>): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var currentWords = 0

        fun flush() {
            val value = normalize(current.toString())
            if (value.isNotBlank()) result += value
            current = StringBuilder()
            currentWords = 0
        }

        sentences.forEach { sentence ->
            val words = sentence.wordCount()
            if (words > MAX_WORDS_PER_SEGMENT) {
                if (currentWords > 0) flush()
                sentence.splitByWordLimit(MAX_WORDS_PER_SEGMENT).forEach { part ->
                    if (part.wordCount() < MIN_WORDS_PER_SEGMENT && result.isNotEmpty()) {
                        result[result.lastIndex] = normalize(result.last() + " " + part)
                    } else {
                        result += part
                    }
                }
            } else if (currentWords == 0 || currentWords + words <= TARGET_WORDS_PER_SEGMENT) {
                if (current.isNotEmpty()) current.append(' ')
                current.append(sentence)
                currentWords += words
            } else {
                flush()
                current.append(sentence)
                currentWords = words
            }
        }
        if (currentWords > 0) flush()

        if (result.size > 1 && result.last().wordCount() < MIN_WORDS_PER_SEGMENT) {
            val tail = result.removeAt(result.lastIndex)
            result[result.lastIndex] = normalize(result.last() + " " + tail)
        }
        return result
    }

    private fun List<String>.mergeTinyTail(): List<String> {
        if (size <= 1 || last().wordCount() >= MIN_WORDS_PER_INTERVAL) return this
        return dropLast(2) + normalize(this[size - 2] + " " + last())
    }

    private fun String.splitByWordLimit(limit: Int): List<String> {
        val words = split(Regex("\\s+")).filter(String::isNotBlank)
        return words.chunked(limit).map { it.joinToString(" ") }
    }

    private fun String.wordCount(): Int = split(Regex("\\s+")).count(String::isNotBlank)
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    private fun weight(text: String): Int = text.count { !it.isWhitespace() }.coerceAtLeast(1)

    private fun newSegment(text: String, rawText: String, startMs: Long, endMs: Long) = TranscriptSegment(
        startMs = startMs,
        endMs = endMs.coerceAtLeast(startMs + 1L),
        rawText = rawText,
        text = text
    )
}
