package com.loomora.core.offlineai.translation

import com.loomora.core.offlineai.live.LiveTranscriptEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinalUtteranceTranslationCoordinatorTest {
    private class FakeEngine : LocalTranslationEngine {
        var translateCalls = 0
        override suspend fun prepare(sourceLanguageTag: String, targetLanguageTag: String) =
            TranslationReadiness.Ready
        override suspend fun translate(text: String, sourceLanguageTag: String, targetLanguageTag: String): String {
            translateCalls += 1
            return "Đã dịch: $text"
        }
        override fun close() = Unit
    }

    @Test
    fun partialCaptionIsNotTranslated() = runTest {
        val engine = FakeEngine()
        val row = FinalUtteranceTranslationCoordinator(engine).toCaption(
            LiveTranscriptEvent.Partial("hello", "en", 10L),
            TranslationSelection(true, "en", "vi")
        )
        assertEquals(0, engine.translateCalls)
        assertNull(row?.translatedText)
    }

    @Test
    fun finalCaptionIsTranslated() = runTest {
        val engine = FakeEngine()
        val row = FinalUtteranceTranslationCoordinator(engine).toCaption(
            LiveTranscriptEvent.Final("s1", "hello", 10L, 20L, "en"),
            TranslationSelection(true, "en", "vi")
        )
        assertEquals(1, engine.translateCalls)
        assertEquals("Đã dịch: hello", row?.translatedText)
    }
}
