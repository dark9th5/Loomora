package com.loomora.core.audio.waveform

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
open class AndroidAudioWaveformDecoder @Inject constructor() : AudioWaveformDecoder {

    override open fun canDecode(sourceFile: File): Boolean = sourceFile.exists()

    override open suspend fun decode(
        sourceFile: File,
        resolution: Int
    ): Result<PersistedWaveform> = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(sourceFile.absolutePath)
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                throw IllegalArgumentException("No audio track found")
            }
            extractor.selectTrack(trackIndex)
            val trackFormat = extractor.getTrackFormat(trackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("Missing audio mime type")
            val durationUs = trackFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
            val durationMs = (durationUs / 1000L).coerceAtLeast(1L)
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val accumulator = PcmWaveformAccumulator(
                resolution = resolution,
                durationMs = durationMs
            )
            var frameOffset = 0L
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            try {
                while (!outputDone) {
                    coroutineContext.ensureActive()
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(BUFFER_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                                ?: throw IllegalStateException("Missing decoder input buffer")
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }

                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, BUFFER_TIMEOUT_US)
                    when {
                        outputIndex >= 0 -> {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                                ?: throw IllegalStateException("Missing decoder output buffer")
                            if (bufferInfo.size > 0) {
                                val chunk = ByteArray(bufferInfo.size)
                                copyOutput(outputBuffer, bufferInfo, chunk)
                                frameOffset += accumulator.appendInterleavedPcm16(
                                    pcm = chunk,
                                    bytesRead = chunk.size,
                                    channelCount = channelCount,
                                    sampleRate = sampleRate,
                                    frameOffset = frameOffset
                                )
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }

                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = codec.outputFormat
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                }
            } finally {
                codec.stop()
                codec.release()
                extractor.release()
            }

            accumulator.build(sourceFingerprint = "")
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        repeat(extractor.trackCount) { index ->
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return index
            }
        }
        return -1
    }

    private fun copyOutput(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        target: ByteArray
    ) {
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        buffer.get(target)
        buffer.clear()
    }

    private companion object {
        const val BUFFER_TIMEOUT_US = 10_000L
    }
}
