package com.loomora.core.offlineai

import kotlinx.serialization.Serializable

enum class ModelCapability {
    TRANSCRIPTION,
    DIARIZATION,
    INSIGHTS,
    SPEECH_ENHANCEMENT
}

enum class RuntimeKind {
    SHERPA_ONNX,
    LITERT_LM
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
    CPU
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
    const val LITERT_LM = "0.10.2"
    const val PIPELINE_VERSION = "offline-pipeline-v1"
    const val TRANSCRIPTION_PIPELINE_VERSION = "offline-transcription-v1"
}

enum class AnalysisJobStatus {
    QUEUED,
    PREPARING_AUDIO,
    ENHANCING,
    DETECTING_SPEECH,
    TRANSCRIBING,
    DIARIZING,
    ALIGNING,
    SUMMARIZING_CHUNKS,
    SYNTHESIZING,
    VALIDATING,
    COMPLETED,
    CANCEL_REQUESTED,
    CANCELLED,
    RETRYABLE_FAILURE,
    TERMINAL_FAILURE
}
