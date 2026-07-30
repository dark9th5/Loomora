package com.loomora.core.offlineai.live

data class LiveTranscriptionConfig(
    val sourceLanguageTag: String?,
    val modelId: String,
    val sampleRateHz: Int = 16_000
)

sealed interface LiveTranscriptEvent {
    data class Partial(
        val text: String,
        val languageTag: String?,
        val startedAtMs: Long
    ) : LiveTranscriptEvent

    data class Final(
        val segmentId: String,
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val languageTag: String?,
        val confidence: Float? = null
    ) : LiveTranscriptEvent

    data class Error(
        val code: String,
        val recoverable: Boolean,
        val message: String? = null
    ) : LiveTranscriptEvent
}

data class LiveCaptionRow(
    val id: String,
    val sourceText: String,
    val translatedText: String? = null,
    val sourceLanguageTag: String?,
    val targetLanguageTag: String? = null,
    val startMs: Long,
    val endMs: Long?,
    val isFinal: Boolean
)
