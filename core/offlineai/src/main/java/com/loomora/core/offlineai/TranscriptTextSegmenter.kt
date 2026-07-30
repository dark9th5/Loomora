package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment

internal object TranscriptTextSegmenter {
    private const val MAX_WORDS_PER_SEGMENT = 18

    fun segment(text: String, startMs: Long, endMs: Long): List<TranscriptSegment> {
        val normalized = text.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return emptyList()

        val chunks = normalized
            .split(Regex("(?<=[.!?…])\\s+"))
            .flatMap(::splitLongChunk)
            .filter(String::isNotBlank)
        if (chunks.size == 1) {
            return listOf(segment(chunks.single(), text, startMs, endMs))
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
            segment(chunk, chunk, chunkStart, chunkEnd.coerceAtLeast(chunkStart + 1))
        }
    }

    fun splitAcrossIntervals(text: String, intervalCount: Int): List<String> {
        if (intervalCount <= 1) return listOf(text.trim())
        val words = text.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        val actualCount = minOf(intervalCount, words.size)
        return List(actualCount) { index ->
            val from = index * words.size / actualCount
            val to = (index + 1) * words.size / actualCount
            words.subList(from, to).joinToString(" ")
        }
    }

    private fun splitLongChunk(chunk: String): List<String> {
        val words = chunk.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.size <= MAX_WORDS_PER_SEGMENT) return listOf(chunk.trim())
        return words.chunked(MAX_WORDS_PER_SEGMENT).map { it.joinToString(" ") }
    }

    private fun weight(text: String): Int = text.count { !it.isWhitespace() }.coerceAtLeast(1)

    private fun segment(text: String, rawText: String, startMs: Long, endMs: Long) = TranscriptSegment(
        startMs = startMs,
        endMs = endMs.coerceAtLeast(startMs + 1),
        rawText = rawText,
        text = text
    )
}
