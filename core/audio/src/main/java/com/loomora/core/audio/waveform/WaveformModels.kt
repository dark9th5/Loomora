package com.loomora.core.audio.waveform

data class PersistedWaveform(
    val sourceFingerprint: String,
    val algorithmVersion: Int,
    val resolution: Int,
    val durationMs: Long,
    val bins: List<Float>
)

sealed interface WaveformLoadState {
    data object Idle : WaveformLoadState
    data object Loading : WaveformLoadState
    data class Ready(val waveform: PersistedWaveform) : WaveformLoadState
    data class Error(val code: WaveformErrorCode) : WaveformLoadState
}

enum class WaveformErrorCode {
    SOURCE_MISSING,
    UNSUPPORTED_FORMAT,
    DECODE_FAILED,
    CORRUPT_INPUT,
    CACHE_IO
}

object WaveformAlgorithm {
    const val VERSION: Int = 1
    const val DETAIL_RESOLUTION: Int = 160
    const val EDITOR_RESOLUTION: Int = 320
}

object WaveformTimelineMapper {
    fun positionMsToBinIndex(
        positionMs: Long,
        durationMs: Long,
        binCount: Int
    ): Int {
        if (binCount <= 0) {
            return 0
        }
        if (durationMs <= 0L) {
            return 0
        }
        val clamped = positionMs.coerceIn(0L, durationMs)
        return ((clamped.toDouble() / durationMs.toDouble()) * binCount.toDouble())
            .toInt()
            .coerceIn(0, binCount - 1)
    }

    fun binIndexToPositionMs(
        index: Int,
        durationMs: Long,
        binCount: Int
    ): Long {
        if (durationMs <= 0L || binCount <= 0) {
            return 0L
        }
        val clampedIndex = index.coerceIn(0, binCount - 1)
        return ((clampedIndex.toDouble() / binCount.toDouble()) * durationMs.toDouble()).toLong()
    }
}
