package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptSpeakerReadabilityTest {
    @Test
    fun compact_doesNotMergeContinuousFastSpeechIntoOneParagraph() {
        val input = (0 until 5).map { index ->
            TranscriptSegment(
                startMs = index * 5_000L,
                endMs = index * 5_000L + 4_900L,
                text = ((index * 20 + 1)..(index * 20 + 20)).joinToString(" ") { "word$it" },
                speakerLabel = "Speaker 1"
            )
        }

        val result = TranscriptSpeakerFusion.compact(input)

        assertTrue(result.size > 1)
        assertTrue(result.all { it.text.wordCount() <= 32 })
        assertEquals(input.joinToString(" ") { it.text }, result.joinToString(" ") { it.text })
    }

    @Test
    fun compact_stillMergesTinyFragmentsFromSameSpeaker() {
        val input = listOf(
            segment(0, 1_000, "hom nay chung ta", "Speaker 1"),
            segment(1_100, 2_000, "hop ve san pham", "Speaker 1")
        )

        val result = TranscriptSpeakerFusion.compact(input)

        assertEquals(1, result.size)
        assertEquals("hom nay chung ta hop ve san pham", result.single().text)
    }

    @Test
    fun displayRows_repairsLegacyOversizedSingleSegment() {
        val longText = (1..100).joinToString(" ") { "word$it" }
        val input = listOf(segment(0, 30_000, longText, "Speaker 1"))

        val result = TranscriptSpeakerFusion.displayRows(input)

        assertTrue(result.size >= 3)
        assertTrue(result.all { it.text.wordCount() in 8..38 })
        assertEquals(longText, result.joinToString(" ") { it.text })
        assertEquals(0L, result.first().startMs)
        assertEquals(30_000L, result.last().endMs)
    }

    @Test
    fun completeSentence_startsANewChatRow() {
        val first = (1..14).joinToString(" ") { "a$it" } + "."
        val second = (1..10).joinToString(" ") { "b$it" }

        val result = TranscriptSpeakerFusion.compact(
            listOf(
                segment(0, 5_000, first, "Speaker 1"),
                segment(5_100, 9_000, second, "Speaker 1")
            )
        )

        assertEquals(2, result.size)
    }

    private fun segment(start: Long, end: Long, text: String, speaker: String) = TranscriptSegment(
        startMs = start,
        endMs = end,
        rawText = text,
        text = text,
        speakerLabel = speaker
    )

    private fun String.wordCount(): Int = split(Regex("\\s+")).count(String::isNotBlank)
}
