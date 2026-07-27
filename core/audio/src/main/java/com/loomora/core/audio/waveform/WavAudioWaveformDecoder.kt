package com.loomora.core.audio.waveform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
open class WavAudioWaveformDecoder @Inject constructor() : AudioWaveformDecoder {

    override open fun canDecode(sourceFile: File): Boolean {
        return sourceFile.extension.equals("wav", ignoreCase = true)
    }

    override open suspend fun decode(
        sourceFile: File,
        resolution: Int
    ): Result<PersistedWaveform> = withContext(Dispatchers.IO) {
        runCatching {
            RandomAccessFile(sourceFile, "r").use { file ->
                val header = parseHeader(file)
                val durationMs = ((header.frameCount * 1000L) / header.sampleRate.toLong()).coerceAtLeast(1L)
                val accumulator = PcmWaveformAccumulator(
                    resolution = resolution,
                    durationMs = durationMs
                )
                file.seek(header.dataOffset)
                val buffer = ByteArray(16 * 1024)
                var frameOffset = 0L
                var remaining = header.dataSize
                while (remaining > 0) {
                    coroutineContext.ensureActive()
                    val read = file.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read <= 0) {
                        break
                    }
                    remaining -= read
                    frameOffset += accumulator.appendInterleavedPcm16(
                        pcm = buffer,
                        bytesRead = read,
                        channelCount = header.channelCount,
                        sampleRate = header.sampleRate,
                        frameOffset = frameOffset
                    )
                }
                accumulator.build(sourceFingerprint = "")
            }
        }
    }

    private fun parseHeader(file: RandomAccessFile): WavHeader {
        val riff = ByteArray(4)
        file.readFully(riff)
        if (!riff.contentEquals("RIFF".toByteArray())) {
            throw IllegalArgumentException("Not a RIFF WAV file")
        }
        file.skipBytes(4)
        val wave = ByteArray(4)
        file.readFully(wave)
        if (!wave.contentEquals("WAVE".toByteArray())) {
            throw IllegalArgumentException("Not a WAVE file")
        }

        var sampleRate = 0
        var channelCount = 0
        var bitsPerSample = 0
        var dataOffset = -1L
        var dataSize = 0

        while (file.filePointer < file.length()) {
            val chunkId = ByteArray(4)
            file.readFully(chunkId)
            val chunkSize = Integer.reverseBytes(file.readInt())
            when (String(chunkId)) {
                "fmt " -> {
                    val audioFormat = java.lang.Short.reverseBytes(file.readShort()).toInt()
                    channelCount = java.lang.Short.reverseBytes(file.readShort()).toInt()
                    sampleRate = Integer.reverseBytes(file.readInt())
                    file.skipBytes(6)
                    bitsPerSample = java.lang.Short.reverseBytes(file.readShort()).toInt()
                    if (chunkSize > 16) {
                        file.skipBytes(chunkSize - 16)
                    }
                    if (audioFormat != 1 || bitsPerSample != 16) {
                        throw IllegalArgumentException("Only PCM 16-bit WAV is supported")
                    }
                }

                "data" -> {
                    dataOffset = file.filePointer
                    dataSize = chunkSize
                    file.skipBytes(chunkSize)
                }

                else -> file.skipBytes(chunkSize)
            }
            if ((chunkSize and 1) == 1) {
                file.skipBytes(1)
            }
        }

        if (sampleRate <= 0 || channelCount <= 0 || dataOffset < 0L || dataSize <= 0) {
            throw EOFException("Incomplete WAV header")
        }

        val frameCount = dataSize / (channelCount * 2)
        return WavHeader(
            sampleRate = sampleRate,
            channelCount = channelCount,
            dataOffset = dataOffset,
            dataSize = dataSize,
            frameCount = frameCount.toLong()
        )
    }

    private data class WavHeader(
        val sampleRate: Int,
        val channelCount: Int,
        val dataOffset: Long,
        val dataSize: Int,
        val frameCount: Long
    )
}
