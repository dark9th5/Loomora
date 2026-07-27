package com.loomora.core.audio.model

sealed interface RecorderState {
    data object Idle : RecorderState
    data object Ready : RecorderState
    data object Preparing : RecorderState
    data class Recording(val recordingId: String, val durationMs: Long) : RecorderState
    data class Paused(val recordingId: String, val durationMs: Long) : RecorderState
    data class Finalizing(val recordingId: String, val durationMs: Long) : RecorderState
    data class Saving(val recordingId: String, val fileUri: String, val durationMs: Long) : RecorderState
    data class Saved(val recordingId: String, val fileUri: String, val durationMs: Long) : RecorderState
    data class Error(
        val type: RecorderErrorType,
        val message: String,
        val recordingId: String? = null,
        val safeSavedPath: String? = null
    ) : RecorderState
}

enum class RecorderErrorType {
    START_FAILED,
    PAUSE_FAILED,
    RESUME_FAILED,
    FINALIZE_FAILED,
    SAVE_FAILED
}

data class RecordingStopResult(
    val recordingId: String,
    val file: java.io.File,
    val durationMs: Long
)
