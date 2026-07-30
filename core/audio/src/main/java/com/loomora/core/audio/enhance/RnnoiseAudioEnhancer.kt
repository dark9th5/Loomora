package com.loomora.core.audio.enhance

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.loomora.core.datastore.NoiseReductionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class RnnoiseAudioEnhancer @Inject constructor() {
    suspend fun enhance(source: File, destination: File, level: NoiseReductionLevel): File? {
        if (level == NoiseReductionLevel.OFF) return null
        return withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()
            val temporary = File(destination.parentFile, "${destination.name}.processing")
            temporary.delete()
            try {
                decodeAndFilter(source, temporary, level.strength)
                if (!temporary.renameTo(destination)) {
                    temporary.copyTo(destination, overwrite = true)
                    temporary.delete()
                }
                destination.takeIf { it.isFile && it.length() > WAV_HEADER_SIZE }
            } catch (error: Exception) {
                Log.e(TAG, "RNNoise enhancement failed for ${source.name}", error)
                temporary.delete()
                destination.delete()
                null
            }
        }
    }

    private suspend fun decodeAndFilter(source: File, destination: File, strength: Float) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(trackIndex)
            val trackFormat = extractor.getTrackFormat(trackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: error("Missing audio MIME type")
            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(trackFormat, null, null, 0)
                codec.start()
                decodeCodec(codec, extractor, destination, strength)
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private suspend fun decodeCodec(
        codec: MediaCodec,
        extractor: MediaExtractor,
        destination: File,
        strength: Float
    ) {
        var sampleRate = 0
        var channelCount = 0
        var inputDone = false
        var outputDone = false
        var dataBytes = 0L
        val frame = ShortArray(RnnoiseProcessor.FRAME_SIZE)
        var frameSize = 0
        val bufferInfo = MediaCodec.BufferInfo()

        FileOutputStream(destination).use { output ->
            output.write(ByteArray(WAV_HEADER_SIZE))
            RnnoiseProcessor().use { processor ->
                while (!outputDone) {
                    coroutineContext.ensureActive()
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(BUFFER_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex) ?: error("Missing codec input")
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, BUFFER_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = codec.outputFormat
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            require(sampleRate == TARGET_SAMPLE_RATE) { "RNNoise requires 48 kHz PCM" }
                            require(channelCount > 0) { "Invalid channel count" }
                        }
                        else -> if (outputIndex >= 0) {
                            val buffer = codec.getOutputBuffer(outputIndex) ?: error("Missing codec output")
                            if (bufferInfo.size > 0) {
                                check(sampleRate == TARGET_SAMPLE_RATE && channelCount > 0)
                                val pcm = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).apply {
                                    position(bufferInfo.offset)
                                    limit(bufferInfo.offset + bufferInfo.size)
                                }.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                while (pcm.remaining() >= channelCount) {
                                    var sum = 0
                                    repeat(channelCount) { sum += pcm.get().toInt() }
                                    frame[frameSize++] = (sum / channelCount).toShort()
                                    if (frameSize == frame.size) {
                                        processor.process(frame, strength)
                                        writePcm16(output, frame, frame.size)
                                        dataBytes += frame.size * 2L
                                        frameSize = 0
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                            outputDone = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        }
                    }
                }
                if (frameSize > 0) {
                    frame.fill(0, frameSize)
                    processor.process(frame, strength)
                    writePcm16(output, frame, frameSize)
                    dataBytes += frameSize * 2L
                }
            }
        }
        writeWavHeader(destination, dataBytes)
    }

    private fun writePcm16(output: FileOutputStream, samples: ShortArray, count: Int) {
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(count) { bytes.putShort(samples[it]) }
        output.write(bytes.array())
    }

    private fun writeWavHeader(file: File, dataBytes: Long) {
        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((36L + dataBytes).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(TARGET_SAMPLE_RATE)
            putInt(TARGET_SAMPLE_RATE * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
        RandomAccessFile(file, "rw").use { wav ->
            wav.seek(0)
            wav.write(header.array())
        }
    }

    companion object {
        private const val TARGET_SAMPLE_RATE = 48_000
        private const val WAV_HEADER_SIZE = 44
        private const val BUFFER_TIMEOUT_US = 10_000L
        private const val TAG = "LoomoraRnnoise"
    }
}
