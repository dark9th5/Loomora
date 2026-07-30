package com.loomora.core.offlineai

import kotlinx.serialization.Serializable

enum class ModelCapability {
    TRANSCRIPTION,
    VOICE_ACTIVITY_DETECTION,
    DIARIZATION,
    INSIGHTS,
    SPEECH_ENHANCEMENT
}

enum class RuntimeKind {
    SHERPA_ONNX,
    LITERT_LM,
    LLAMA_CPP
}

enum class ModelInstallState {
    NOT_INSTALLED,
    IMPORTING,
    VERIFYING,
    READY,
    INCOMPATIBLE,
    CORRUPT,
    REMOVING,
    ERROR
}

enum class ExecutionBackend {
    CPU,
    GPU,
    NPU
}

enum class CompatibilityIssue {
    ABI_UNSUPPORTED,
    RAM_INSUFFICIENT,
    STORAGE_INSUFFICIENT,
    BACKEND_UNSUPPORTED
}

sealed interface CompatibilityResult {
    data class Compatible(
        val backend: ExecutionBackend
    ) : CompatibilityResult

    data class Incompatible(
        val issue: CompatibilityIssue,
        val detail: String
    ) : CompatibilityResult
}

@Serializable
data class OfflineModelManifest(
    val id: String,
    val version: String,
    val capability: ModelCapability,
    val runtime: RuntimeKind,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val minimumRamMb: Int? = null,
    val supportedAbis: Set<String>,
    val supportedLanguages: Set<String>,
    val licenseName: String,
    val licenseUrl: String? = null,
    val sourceUrl: String? = null,
    val pipelineCompatibility: String,
    val additionalFiles: List<OfflineModelFile> = emptyList()
)

@Serializable
data class OfflineModelFile(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String
)

data class OfflineModelRecord(
    val manifest: OfflineModelManifest,
    val state: ModelInstallState,
    val installedPath: String?,
    val installedAt: Long?,
    val lastVerifiedAt: Long?,
    val compatibility: CompatibilityResult,
    val errorCode: String? = null
)

object OfflineAiRuntimeVersions {
    const val SHERPA_ONNX = "1.13.4"
    const val LITERT_LM = "0.14.0"
    const val PIPELINE_VERSION = "offline-pipeline-v2"
    const val TRANSCRIPTION_PIPELINE_VERSION = "offline-transcription-v2"
    const val DIARIZATION_PIPELINE_VERSION = "offline-diarization-v2"
    const val FUSION_PIPELINE_VERSION = "offline-transcript-fusion-v2"
    const val INSIGHTS_PIPELINE_VERSION = "offline-insights-v2"
    const val INSIGHTS_PROMPT_VERSION = "meeting-insights-prompt-v2"
    const val INSIGHTS_SCHEMA_VERSION = "meeting-insights-schema-v2"
    const val EXTRACTIVE_INSIGHTS_MODEL_ID = "local-extractive-insights"
    const val EXTRACTIVE_INSIGHTS_MODEL_VERSION = "1"
    const val HEURISTIC_INSIGHTS_MODEL_ID = "local-heuristic-insights"
    const val HEURISTIC_INSIGHTS_MODEL_VERSION = "1"
    const val HYBRID_INSIGHTS_MODEL_ID = "local-hybrid-insights"
    const val HYBRID_INSIGHTS_MODEL_VERSION = "1"
}

enum class AnalysisJobStatus {
    QUEUED,
    RUNNING,
    CANCELLED,
    COMPLETED,
    RETRYABLE_FAILURE,
    TERMINAL_FAILURE,
    INVALIDATED,
    PREPARING_AUDIO,
    ENHANCING,
    DETECTING_SPEECH,
    TRANSCRIBING,
    DIARIZING,
    ALIGNING,
    SUMMARIZING_CHUNKS,
    SYNTHESIZING,
    VALIDATING,
    CANCEL_REQUESTED,
    PUBLISHING,
    CLEANING_UP
}

enum class OfflineAnalysisStage {
    QUEUED,
    PREPARING_AUDIO,
    ENHANCING,
    DETECTING_SPEECH,
    TRANSCRIBING,
    DIARIZING,
    ALIGNING,
    GENERATING_HEURISTIC_INSIGHTS,
    OPTIONAL_LLM_ENHANCEMENT,
    VALIDATING,
    PUBLISHING,
    CLEANING_UP
}

enum class InsightGenerationMode {
    HEURISTIC,
    LLM_ENHANCED,
    HEURISTIC_FALLBACK
}

enum class InsightCompletionQuality {
    EXTRACTIVE_ONLY,
    ENHANCED,
    DEGRADED_BUT_VALID
}

@Serializable
data class OfflineAiStageTimings(
    val fingerprintMs: Long = 0,
    val decodeAndResampleMs: Long = 0,
    val speechDetectionMs: Long = 0,
    val recognizerLoadMs: Long = 0,
    val transcriptionMs: Long = 0,
    val diarizationMs: Long = 0,
    val alignmentMs: Long = 0,
    val heuristicInsightsMs: Long = 0,
    val optionalLlmMs: Long = 0,
    val totalMs: Long = 0
)
