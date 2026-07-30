package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment

internal object TranscriptHallucinationFilter {
    private const val MAX_PHRASE_WORDS = 12

    fun clean(segments: List<TranscriptSegment>): List<TranscriptSegment> {
        if (segments.isEmpty()) return emptyList()
        val joinedText = segments.joinToString(" ") { it.text.trim() }.trim()
        val cleanedJoinedText = collapseRepeatedPhrases(joinedText)
        if (cleanedJoinedText != joinedText) {
            return listOf(
                TranscriptSegment(
                    startMs = segments.minOf { it.startMs },
                    endMs = segments.maxOf { it.endMs },
                    text = cleanedJoinedText,
                    rawText = segments.joinToString(" ") { it.rawText.trim() }.trim()
                )
            )
        }
        return segments.mapNotNull { segment ->
            val cleaned = collapseRepeatedPhrases(segment.text)
            cleaned.takeIf(String::isNotBlank)?.let { segment.copy(text = it) }
        }
    }

    internal fun collapseRepeatedPhrases(text: String): String {
        val words = text.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.size < 3) return text.trim()
        val result = mutableListOf<String>()
        var index = 0
        while (index < words.size) {
            var repeatedPhraseSize = 0
            var repeatedCount = 0
            val maxPhraseSize = minOf(MAX_PHRASE_WORDS, (words.size - index) / 3)
            for (phraseSize in 1..maxPhraseSize) {
                val phrase = words.subList(index, index + phraseSize).map(::normalizeWord)
                var count = 1
                while (index + (count + 1) * phraseSize <= words.size) {
                    val candidate = words.subList(
                        index + count * phraseSize,
                        index + (count + 1) * phraseSize
                    ).map(::normalizeWord)
                    if (candidate != phrase) break
                    count++
                }
                val minimumRepeats = if (phraseSize == 1) 4 else 3
                if (count >= minimumRepeats && count * phraseSize > repeatedCount * repeatedPhraseSize) {
                    repeatedPhraseSize = phraseSize
                    repeatedCount = count
                }
            }
            if (repeatedCount > 0) {
                result += words.subList(index, index + repeatedPhraseSize)
                index += repeatedPhraseSize * repeatedCount
            } else {
                result += words[index]
                index++
            }
        }
        return result.joinToString(" ")
    }

    private fun normalizeWord(word: String): String = word
        .lowercase()
        .trim { !it.isLetterOrDigit() }
}
