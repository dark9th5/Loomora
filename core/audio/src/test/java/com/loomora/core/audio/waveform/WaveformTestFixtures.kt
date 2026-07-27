package com.loomora.core.audio.waveform

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object WaveformTestFixtures {

    fun writeWavFile(
        target: File,
        sampleRate: Int = 16_000,
        channelCount: Int = 1,
        durationMs: Long,
        sampleAtFrame: (Long) -> Short
    ): File {
        val totalFrames = ((sampleRate.toLong() * durationMs) / 1000L).toInt().coerceAtLeast(1)
        val dataSize = totalFrames * channelCount * 2
        RandomAccessFile(target, "rw").use { file ->
            file.setLength(0L)
            file.writeBytes("RIFF")
            file.writeInt(Integer.reverseBytes(36 + dataSize))
            file.writeBytes("WAVE")
            file.writeBytes("fmt ")
            file.writeInt(Integer.reverseBytes(16))
            file.writeShort(java.lang.Short.reverseBytes(1).toInt())
            file.writeShort(java.lang.Short.reverseBytes(channelCount.toShort()).toInt())
            file.writeInt(Integer.reverseBytes(sampleRate))
            file.writeInt(Integer.reverseBytes(sampleRate * channelCount * 2))
            file.writeShort(java.lang.Short.reverseBytes((channelCount * 2).toShort()).toInt())
            file.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
            file.writeBytes("data")
            file.writeInt(Integer.reverseBytes(dataSize))
            repeat(totalFrames) { frame ->
                val sample = sampleAtFrame(frame.toLong())
                repeat(channelCount) {
                    file.writeShort(java.lang.Short.reverseBytes(sample).toInt())
                }
            }
        }
        return target
    }

    fun silence(target: File, durationMs: Long): File {
        return writeWavFile(target = target, durationMs = durationMs) { 0 }
    }

    fun constantTone(target: File, durationMs: Long, amplitude: Double = 0.5): File {
        return writeWavFile(target = target, durationMs = durationMs) {
            (Short.MAX_VALUE * amplitude).roundToInt().toShort()
        }
    }

    fun sineTone(target: File, durationMs: Long, frequencyHz: Double = 440.0, amplitude: Double = 0.8): File {
        val sampleRate = 16_000
        return writeWavFile(target = target, durationMs = durationMs, sampleRate = sampleRate) { frame ->
            val radians = (2.0 * PI * frequencyHz * frame.toDouble()) / sampleRate.toDouble()
            (sin(radians) * Short.MAX_VALUE * amplitude).roundToInt().toShort()
        }
    }
}
