package com.loomora.core.offlineai.translation

import com.loomora.core.offlineai.live.LiveCaptionRow
import com.loomora.core.offlineai.live.LiveTranscriptEvent

/**
 * Translation is intentionally triggered for finalized utterances by default.
 * This avoids continuously changing partial translations while a person is speaking.
 */
class FinalUtteranceTranslationCoordinator(
    private val engine: LocalTranslationEngine
) {
    suspend fun toCaption(
        event: LiveTranscriptEvent,
        selection: TranslationSelection
    ): LiveCaptionRow? {
        val config = selection.validated()
        return when (event) {
            is LiveTranscriptEvent.Partial -> LiveCaptionRow(
                id = "partial-${event.startedAtMs}",
                sourceText = event.text,
                sourceLanguageTag = event.languageTag ?: config.sourceLanguageTag,
                startMs = event.startedAtMs,
                endMs = null,
                isFinal = false
            )
            is LiveTranscriptEvent.Final -> {
                val source = event.languageTag
                    ?: config.sourceLanguageTag
                    ?: return LiveCaptionRow(
                        id = event.segmentId,
                        sourceText = event.text,
                        sourceLanguageTag = null,
                        startMs = event.startMs,
                        endMs = event.endMs,
                        isFinal = true
                    )
                val target = config.targetLanguageTag
                val translated = if (config.enabled && target != null && source != target) {
                    when (engine.prepare(source, target)) {
                        TranslationReadiness.Ready -> engine.translate(event.text, source, target)
                        else -> null
                    }
                } else {
                    null
                }
                LiveCaptionRow(
                    id = event.segmentId,
                    sourceText = event.text,
                    translatedText = translated,
                    sourceLanguageTag = source,
                    targetLanguageTag = target,
                    startMs = event.startMs,
                    endMs = event.endMs,
                    isFinal = true
                )
            }
            is LiveTranscriptEvent.Error -> null
        }
    }
}
