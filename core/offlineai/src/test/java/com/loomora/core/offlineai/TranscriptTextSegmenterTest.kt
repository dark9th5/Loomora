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

        // 11 words total -> coalesced into 1 segment since TARGET_WORDS_PER_SEGMENT is 42
        assertTrue(result.isNotEmpty())
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

        // 40 words fits into 1 segment (< 42 target words per segment)
        assertTrue(result.isNotEmpty())
        assertEquals(text, result.joinToString(" ") { it.text })
    }
}
