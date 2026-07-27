package com.loomora.core.offlineai

import com.loomora.core.model.AiInsights
import com.loomora.core.model.TranscriptSegment
import java.io.Closeable
import java.io.File

interface LocalTranscriptionEngine : Closeable {
    suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput
}

interface LocalDiarizationEngine : Closeable {
    suspend fun diarize(audioFile: File, transcript: List<TranscriptSegment>): List<TranscriptSegment>
}

interface LocalMeetingInsightEngine : Closeable {
    suspend fun analyze(transcript: List<TranscriptSegment>): AiInsights
}

interface LocalSpeechEnhancementEngine : Closeable {
    suspend fun enhance(audioFile: File): File
}

data class TranscriptionInput(
    val pcm16kMonoFile: File,
    val originalAudioFile: File,
    val sourceFingerprint: String,
    val languageHint: String?,
    val model: OfflineModelRecord,
    val speechWindows: List<SpeechWindow>
)

data class TranscriptionOutput(
    val segments: List<TranscriptSegment>,
    val modelId: String,
    val modelVersion: String,
    val languageTag: String?,
    val processingDurationMs: Long,
    val memoryObservationKb: Long?
)

data class SpeechWindow(
    val startMs: Long,
    val endMs: Long
)
