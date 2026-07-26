package com.loomora.core.audio.model

sealed interface RecorderState {
    data object Idle : RecorderState
    data object Preparing : RecorderState
    data class Recording(val durationMs: Long) : RecorderState
    data class Paused(val durationMs: Long) : RecorderState
    data object Finalizing : RecorderState
    data class Completed(val recordingId: String, val fileUri: String) : RecorderState
    data class Error(val message: String, val safeSavedPath: String? = null) : RecorderState
}
