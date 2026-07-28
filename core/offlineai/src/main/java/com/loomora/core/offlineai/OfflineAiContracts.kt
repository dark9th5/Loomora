package com.loomora.core.offlineai

import com.loomora.core.model.AiInsights
import com.loomora.core.model.InsightRevision
import com.loomora.core.model.SpeakerTurn
import com.loomora.core.model.TranscriptRevision
import com.loomora.core.model.TranscriptSegment
import java.io.Closeable
import java.io.File

interface LocalTranscriptionEngine : Closeable {
    suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput
}

interface LocalDiarizationEngine : Closeable {
    suspend fun diarize(input: DiarizationInput): DiarizationOutput
}

interface LocalMeetingInsightEngine : Closeable {
    suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput
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

data class DiarizationInput(
    val pcm16kMonoFile: File,
    val originalAudioFile: File,
    val sourceFingerprint: String,
    val model: OfflineModelRecord,
    val clustering: DiarizationClusteringSettings
)

data class DiarizationOutput(
    val turns: List<SpeakerTurn>,
    val modelId: String,
    val modelVersion: String,
    val clusteringSettings: DiarizationClusteringSettings,
    val processingDurationMs: Long,
    val memoryObservationKb: Long?
)

data class DiarizationClusteringSettings(
    val numClusters: Int = -1,
    val threshold: Float = 0.5f,
    val minDurationOnSec: Float = 0.3f,
    val minDurationOffSec: Float = 0.5f,
    val numThreads: Int = 2,
    val provider: String = "cpu"
)

data class MeetingInsightInput(
    val transcriptRevision: TranscriptRevision,
    val model: OfflineModelRecord?,
    val languageTag: String?,
    val backendPolicy: LiteRtLmBackendPolicy = LiteRtLmBackendPolicy()
)

data class MeetingInsightOutput(
    val insights: AiInsights,
    val modelId: String,
    val modelVersion: String,
    val promptVersion: String,
    val schemaVersion: String,
    val pipelineVersion: String,
    val languageTag: String?,
    val chunkCheckpoints: List<InsightChunkCheckpoint>,
    val modelSizeBytes: Long,
    val loadTimeMs: Long,
    val generationTimeMs: Long,
    val memoryObservationKb: Long?,
    val generationMode: InsightGenerationMode = InsightGenerationMode.HEURISTIC,
    val completionQuality: InsightCompletionQuality = InsightCompletionQuality.EXTRACTIVE_ONLY,
    val usedHeuristicFallback: Boolean = false,
    val fallbackReason: String? = null
)

data class InsightChunkCheckpoint(
    val chunkIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val segmentIds: List<String>,
    val outputJson: String
)

data class LiteRtLmBackendPolicy(
    val preferred: List<ExecutionBackend> = listOf(ExecutionBackend.CPU),
    val fallback: List<ExecutionBackend> = listOf(ExecutionBackend.CPU),
    val maxTokens: Int = 4096,
    val generationTimeoutMs: Long = 120_000L,
    val outputLanguageTag: String? = null
)

data class LlamaCppInsightPolicy(
    val lowMemoryModelId: String = "qwen2.5-0.5b-instruct-q4-gguf",
    val standardModelId: String = "qwen2.5-1.5b-instruct-q4-gguf",
    val maxContextTokens: Int = 4096,
    val maxOutputTokens: Int = 768,
    val grammarName: String = "meeting-insights-json-v1",
    val unloadSpeechModelsBeforeLoad: Boolean = true
)

data class SpeechWindow(
    val startMs: Long,
    val endMs: Long
)
