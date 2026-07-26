package com.loomora.core.model

data class Recording(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val durationMs: Long,
    val status: RecordingStatus = RecordingStatus.SAVED,
    val originalFileUri: String,
    val editedOutputUri: String? = null,
    val mimeType: String = "audio/aac",
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val bitrate: Int = 128000,
    val sizeBytes: Long = 0L,
    val languageHint: String = "en",
    val isFavorite: Boolean = false,
    val deletedAt: Long? = null,
    val recoveryState: String = "NORMAL",
    val transcriptStatus: String = "NOT_STARTED",
    val insightStatus: String = "NOT_STARTED"
)

enum class RecordingStatus {
    RECORDING,
    PAUSED,
    FINALIZING,
    SAVED,
    RECOVERY_FAILED
}
