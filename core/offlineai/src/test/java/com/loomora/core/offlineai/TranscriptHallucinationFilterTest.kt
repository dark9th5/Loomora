package com.loomora.core.offlineai

import com.loomora.core.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptHallucinationFilterTest {
    @Test
    fun repeatedMultiWordPhrase_isCollapsed() {
        val text = "một quái lần này, một quái lần này, một quái lần này, một quái lần này"

        assertEquals("một quái lần này,", TranscriptHallucinationFilter.collapseRepeatedPhrases(text))
    }

    @Test
    fun ordinaryIntentionalRepetition_isPreserved() {
        val text = "rất rất quan trọng và cần nhắc lại hai lần"

        assertEquals(text, TranscriptHallucinationFilter.collapseRepeatedPhrases(text))
    }

    @Test
    fun cleaning_preservesRawModelOutputForAudit() {
        val segment = TranscriptSegment(
            startMs = 0,
            endMs = 1_000,
            rawText = "xin chào xin chào xin chào",
            text = "xin chào xin chào xin chào"
        )

        val cleaned = TranscriptHallucinationFilter.clean(listOf(segment)).single()

        assertEquals("xin chào", cleaned.text)
        assertEquals(segment.rawText, cleaned.rawText)
    }

    @Test
    fun repetitionSplitAcrossTimestampSegments_isCollapsedGlobally() {
        val segments = listOf(
            TranscriptSegment(startMs = 0L, endMs = 10_000L, text = "Cái đó là một quái lần này, một quái lần", rawText = "raw-1"),
            TranscriptSegment(startMs = 10_000L, endMs = 20_000L, text = "này, một quái lần", rawText = "raw-2"),
            TranscriptSegment(startMs = 20_000L, endMs = 30_000L, text = "này, một", rawText = "raw-3")
        )

        val cleaned = TranscriptHallucinationFilter.clean(segments).single()

        assertEquals("Cái đó là một quái lần này, một", cleaned.text)
        assertEquals("raw-1 raw-2 raw-3", cleaned.rawText)
        assertEquals(0L, cleaned.startMs)
        assertEquals(30_000L, cleaned.endMs)
    }
}
