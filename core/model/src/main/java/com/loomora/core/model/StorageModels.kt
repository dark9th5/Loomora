package com.loomora.core.model

data class StorageUsageSummary(
    val recordingsBytes: Long = 0L,
    val exportsBytes: Long = 0L,
    val tempBytes: Long = 0L,
    val modelsBytes: Long = 0L,
    val freeBytes: Long = 0L
)

sealed interface RecordingOperationResult {
    data object Success : RecordingOperationResult
    data object NotFound : RecordingOperationResult
    data object SourceMissing : RecordingOperationResult
    data object ExportCancelled : RecordingOperationResult
    data class LowStorage(val requiredBytes: Long, val availableBytes: Long) : RecordingOperationResult
    data class FileSystemFailure(val detail: String) : RecordingOperationResult
    data class DatabaseFailure(val detail: String) : RecordingOperationResult
}
