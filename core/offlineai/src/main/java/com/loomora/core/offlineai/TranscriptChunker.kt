package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptChunker @Inject constructor() {
    fun chunk(
        segments: List<TranscriptSegment>,
        maxChars: Int = DEFAULT_MAX_CHARS
    ): List<TranscriptChunk> {
        val valid = segments.filter { it.id.isNotBlank() && it.text.isNotBlank() }
            .sortedWith(compareBy({ it.startMs }, { it.endMs }))
        if (valid.isEmpty()) return emptyList()
        val chunks = mutableListOf<TranscriptChunk>()
        var current = mutableListOf<TranscriptSegment>()
        var currentChars = 0
        valid.forEach { segment ->
            val nextSize = segment.text.length + 32
            if (current.isNotEmpty() && currentChars + nextSize > maxChars) {
                chunks += current.toChunk(chunks.size)
                current = mutableListOf()
                currentChars = 0
            }
            current += segment
            currentChars += nextSize
        }
        if (current.isNotEmpty()) chunks += current.toChunk(chunks.size)
        return chunks
    }

    private fun List<TranscriptSegment>.toChunk(index: Int): TranscriptChunk {
        return TranscriptChunk(
            index = index,
            startMs = minOf { it.startMs },
            endMs = maxOf { it.endMs },
            segments = this
        )
    }

    private companion object {
        const val DEFAULT_MAX_CHARS = 6_000
    }
}

data class TranscriptChunk(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val segments: List<TranscriptSegment>
) {
    val segmentIds: List<String> = segments.map { it.id }
}
