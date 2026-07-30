package com.loomora.core.offlineai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptTextSegmenterTest {
    @Test
    fun vietnameseSentences_areSeparatedAndKeepAllText() {
        val result = TranscriptTextSegmenter.segment(
            "Xin chao. Hom nay chung ta hop ve san pham? Dong y!",
            startMs = 1_000,
            endMs = 7_000
        )

        assertEquals(3, result.size)
        assertEquals(1_000L, result.first().startMs)
        assertEquals(7_000L, result.last().endMs)
        assertEquals(
            "Xin chao. Hom nay chung ta hop ve san pham? Dong y!",
            result.joinToString(" ") { it.text }
        )
    }

    @Test
    fun unpunctuatedLongSpeech_isSplitIntoReadableRows() {
        val text = (1..40).joinToString(" ") { "word$it" }
        val result = TranscriptTextSegmenter.segment(text, 0, 8_000)

        assertEquals(3, result.size)
        assertTrue(result.all { it.text.split(" ").size <= 18 })
        assertEquals(text, result.joinToString(" ") { it.text })
    }
}
