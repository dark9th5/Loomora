package com.loomora.core.audio

sealed interface RecorderState {
    data object Idle : RecorderState
    data object Preparing : RecorderState
    data class Recording(val durationMs: Long, val currentAmplitude: Float) : RecorderState
    data class Paused(val durationMs: Long) : RecorderState
    data object Stopping : RecorderState
    data object Finalizing : RecorderState
    data class Completed(val recordingId: String, val filePath: String, val durationMs: Long) : RecorderState
    data class Error(val message: String, val isRecoverable: Boolean) : RecorderState
}

interface AudioEngine {
    fun getRecorderState(): RecorderState
    fun isValidTransition(from: RecorderState, to: RecorderState): Boolean
}
