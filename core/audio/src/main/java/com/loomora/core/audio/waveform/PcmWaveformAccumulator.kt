package com.loomora.core.audio.waveform

import kotlin.math.abs
import kotlin.math.max

internal class PcmWaveformAccumulator(
    private val resolution: Int,
    durationMs: Long
) {
    private val safeResolution = resolution.coerceAtLeast(1)
    private val safeDurationMs = durationMs.coerceAtLeast(1L)
    private val bins = FloatArray(safeResolution)
    private var maxAmplitude = 0f

    fun appendInterleavedPcm16(
        pcm: ByteArray,
        bytesRead: Int,
        channelCount: Int,
        sampleRate: Int,
        frameOffset: Long
    ): Long {
        val safeChannelCount = channelCount.coerceAtLeast(1)
        val frameSizeBytes = safeChannelCount * 2
        if (frameSizeBytes <= 0 || sampleRate <= 0) {
            return 0L
        }

        val frameCount = bytesRead / frameSizeBytes
        var byteIndex = 0
        repeat(frameCount) { frameIndex ->
            var framePeak = 0f
            repeat(safeChannelCount) {
                val sample = ((pcm[byteIndex + 1].toInt() shl 8) or (pcm[byteIndex].toInt() and 0xff)).toShort()
                framePeak = max(framePeak, abs(sample.toInt()) / 32767f)
                byteIndex += 2
            }
            val absoluteFrame = frameOffset + frameIndex
            val timeMs = (absoluteFrame * 1000L) / sampleRate.toLong()
            val binIndex = WaveformTimelineMapper.positionMsToBinIndex(timeMs, safeDurationMs, safeResolution)
            bins[binIndex] = max(bins[binIndex], framePeak)
            maxAmplitude = max(maxAmplitude, framePeak)
        }
        return frameCount.toLong()
    }

    fun build(sourceFingerprint: String): PersistedWaveform {
        val normalizedBins = if (maxAmplitude <= 0f) {
            bins.map { 0f }
        } else {
            bins.map { (it / maxAmplitude).coerceIn(0f, 1f) }
        }
        return PersistedWaveform(
            sourceFingerprint = sourceFingerprint,
            algorithmVersion = WaveformAlgorithm.VERSION,
            resolution = safeResolution,
            durationMs = safeDurationMs,
            bins = normalizedBins
        )
    }
}
