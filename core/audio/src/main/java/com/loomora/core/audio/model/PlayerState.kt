package com.loomora.core.audio.model

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Buffering : PlayerState
    data class Playing(val positionMs: Long, val durationMs: Long, val speed: Float = 1.0f) : PlayerState
    data class Paused(val positionMs: Long, val durationMs: Long, val speed: Float = 1.0f) : PlayerState
    data class Error(val message: String) : PlayerState
}
