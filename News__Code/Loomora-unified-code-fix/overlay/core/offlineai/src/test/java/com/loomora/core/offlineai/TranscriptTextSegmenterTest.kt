package com.loomora.core.offlineai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptTextSegmenterTest {
    @Test
    fun vietnameseSentences_keepAllTextAndSafeTiming() {
        val text = "Xin chao. Hom nay chung ta hop ve san pham? Dong y!"
        val result = TranscriptTextSegmenter.segment(text, startMs = 1_000, endMs = 7_000)

        assertTrue(result.isNotEmpty())
        assertEquals(1_000L, result.first().startMs)
        assertEquals(7_000L, result.last().endMs)
        assertEquals(text, result.joinToString(" ") { it.text })
        assertTrue(result.zipWithNext().all { (a, b) -> a.endMs <= b.startMs })
    }

    @Test
    fun unpunctuatedFastSpeech_isSplitIntoReadableRows() {
        val text = (1..120).joinToString(" ") { "word$it" }
        val result = TranscriptTextSegmenter.segment(text, 0, 40_000)

        assertTrue(result.size >= 4)
        assertTrue(result.all { it.text.wordCount() in 8..38 })
        assertEquals(text, result.joinToString(" ") { it.text })
        assertEquals(0L, result.first().startMs)
        assertEquals(40_000L, result.last().endMs)
    }

    @Test
    fun commaNearTarget_isPreferredOverArbitraryBoundary() {
        val firstClause = (1..24).joinToString(" ") { "a$it" } + ","
        val secondClause = (25..55).joinToString(" ") { "a$it" }
        val result = TranscriptTextSegmenter.segment("$firstClause $secondClause", 0, 20_000)

        assertTrue(result.size >= 2)
        assertTrue(result.first().text.endsWith(","))
        assertEquals("$firstClause $secondClause", result.joinToString(" ") { it.text })
    }

    @Test
    fun tinyTail_isBalancedInsteadOfCreatingTwoWordRow() {
        val text = (1..42).joinToString(" ") { "word$it" }
        val result = TranscriptTextSegmenter.segment(text, 0, 12_000)

        assertTrue(result.size >= 2)
        assertTrue(result.all { it.text.wordCount() >= 8 })
        assertEquals(text, result.joinToString(" ") { it.text })
    }

    private fun String.wordCount(): Int = split(Regex("\\s+")).count(String::isNotBlank)
}
