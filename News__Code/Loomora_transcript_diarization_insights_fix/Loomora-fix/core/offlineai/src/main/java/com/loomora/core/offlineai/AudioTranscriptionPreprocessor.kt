package com.loomora.core.offlineai

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.TimeSource

@Singleton
open class AudioTranscriptionPreprocessor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    open suspend fun fingerprint(sourceFile: File): String = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || !sourceFile.isFile) {
            throw OfflineAiException.FileMissing
        }
        sha256(sourceFile)
    }

    open suspend fun prepare(sourceFile: File): PreparedTranscriptionAudio {
        return prepare(sourceFile, sourceFingerprint = null)
    }

    open suspend fun prepare(
        sourceFile: File,
        sourceFingerprint: String?
    ): PreparedTranscriptionAudio = prepare(sourceFile, sourceFingerprint, vadModel = null)

    open suspend fun prepare(
        sourceFile: File,
        sourceFingerprint: String?,
        vadModel: OfflineModelRecord?
    ): PreparedTranscriptionAudio = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || !sourceFile.isFile) {
            throw OfflineAiException.FileMissing
        }

        val resolvedFingerprint = sourceFingerprint ?: sha256(sourceFile)
        val tempDir = File(context.cacheDir, "offline-ai/transcription").apply { mkdirs() }
        val tempPcm = File(tempDir, "${sourceFile.nameWithoutExtension}-${System.nanoTime()}.pcm")

        try {
            val decodeMark = TimeSource.Monotonic.markNow()
            if (sourceFile.extension.equals("wav", ignoreCase = true)) {
                val wav = parseWavHeader(sourceFile)
                streamWavToPcm16kMono(sourceFile, wav, tempPcm)
            } else {
                streamAndroidAudioToPcm16kMono(sourceFile, tempPcm)
            }
            val decodeAndResampleMs = decodeMark.elapsedNow().inWholeMilliseconds
            val speechDetectionMark = TimeSource.Monotonic.markNow()
            val windows = detectSpeechWindows(tempPcm, vadModel)
            val speechDetectionMs = speechDetectionMark.elapsedNow().inWholeMilliseconds
            PreparedTranscriptionAudio(
                sourceFile = sourceFile,
                sourceFingerprint = resolvedFingerprint,
                pcm16kMonoFile = tempPcm,
                speechWindows = windows,
                decodeAndResampleMs = decodeAndResampleMs,
                speechDetectionMs = speechDetectionMs
            )
        } catch (cancellation: CancellationException) {
            tempPcm.delete()
            throw cancellation
        } catch (error: OfflineAiException) {
            tempPcm.delete()
            throw error
        } catch (_: Exception) {
            tempPcm.delete()
            throw OfflineAiException.FileCorrupt
        }
    }

    private suspend fun streamAndroidAudioToPcm16kMono(
        sourceFile: File,
        destination: File
    ) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(sourceFile.absolutePath)
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                throw OfflineAiException.FileCorrupt
            }
            extractor.selectTrack(trackIndex)
            val trackFormat = extractor.getTrackFormat(trackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                ?: throw OfflineAiException.FileCorrupt
            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(trackFormat, null, null, 0)
                codec.start()

                var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val bufferInfo = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                var sourceFrameOffset = 0L
                var nextOutputFrame = 0L

                FileOutputStream(destination).use { output ->
                    while (!outputDone) {
                        coroutineContext.ensureActive()
                        if (!inputDone) {
                            val inputIndex = codec.dequeueInputBuffer(BUFFER_TIMEOUT_US)
                            if (inputIndex >= 0) {
                                val inputBuffer = codec.getInputBuffer(inputIndex)
                                    ?: throw OfflineAiException.FileCorrupt
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

                        when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, BUFFER_TIMEOUT_US)) {
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val outputFormat = codec.outputFormat
                                sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                if (
                                    outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                                    outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) != AudioFormat.ENCODING_PCM_16BIT
                                ) {
                                    throw OfflineAiException.FileCorrupt
                                }
                            }

                            else -> if (outputIndex >= 0) {
                                val outputBuffer = codec.getOutputBuffer(outputIndex)
                                    ?: throw OfflineAiException.FileCorrupt
                                if (bufferInfo.size > 0) {
                                    val chunk = ByteArray(bufferInfo.size)
                                    copyOutput(outputBuffer, bufferInfo, chunk)
                                    val result = writeInterleavedPcm16As16kMono(
                                        pcm = chunk,
                                        bytesRead = chunk.size,
                                        channelCount = channelCount,
                                        sampleRate = sampleRate,
                                        sourceFrameOffset = sourceFrameOffset,
                                        nextOutputFrame = nextOutputFrame,
                                        output = output
                                    )
                                    sourceFrameOffset += result.sourceFramesRead
                                    nextOutputFrame = result.nextOutputFrame
                                }
                                codec.releaseOutputBuffer(outputIndex, false)
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    outputDone = true
                                }
                            }
                        }
                    }
                    output.flush()
                }
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    fun cleanup(prepared: PreparedTranscriptionAudio?) {
        prepared?.pcm16kMonoFile?.delete()
    }

    private suspend fun streamWavToPcm16kMono(
        sourceFile: File,
        header: WavHeader,
        destination: File
    ) {
        RandomAccessFile(sourceFile, "r").use { input ->
            FileOutputStream(destination).use { output ->
                input.seek(header.dataOffset)
                val frameSize = header.channelCount * 2
                val buffer = ByteArray(frameSize * 2048)
                var sourceFrameOffset = 0L
                var remaining = header.dataSize
                var nextOutputFrame = 0L
                while (remaining > 0) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read <= 0) break
                    remaining -= read
                    val result = writeInterleavedPcm16As16kMono(
                        pcm = buffer,
                        bytesRead = read,
                        channelCount = header.channelCount,
                        sampleRate = header.sampleRate,
                        sourceFrameOffset = sourceFrameOffset,
                        nextOutputFrame = nextOutputFrame,
                        output = output
                    )
                    sourceFrameOffset += result.sourceFramesRead
                    nextOutputFrame = result.nextOutputFrame
                }
                output.flush()
            }
        }
    }

    private fun writeInterleavedPcm16As16kMono(
        pcm: ByteArray,
        bytesRead: Int,
        channelCount: Int,
        sampleRate: Int,
        sourceFrameOffset: Long,
        nextOutputFrame: Long,
        output: FileOutputStream
    ): ResampleWriteResult {
        if (sampleRate <= 0 || channelCount <= 0) {
            throw OfflineAiException.FileCorrupt
        }
        val frameSize = channelCount * 2
        val framesRead = bytesRead / frameSize
        var nextTargetFrame = nextOutputFrame
        var frame = 0
        while (frame < framesRead) {
            val sourceFrame = sourceFrameOffset + frame
            val targetFrame = (sourceFrame * TARGET_SAMPLE_RATE) / sampleRate
            if (targetFrame >= nextTargetFrame) {
                val mono = readMonoPcm16(pcm, frame * frameSize, channelCount)
                writeLittleEndianShort(output, mono)
                nextTargetFrame = targetFrame + 1
            }
            frame++
        }
        return ResampleWriteResult(
            sourceFramesRead = framesRead.toLong(),
            nextOutputFrame = nextTargetFrame
        )
    }

    private suspend fun detectSpeechWindows(
        pcmFile: File,
        vadModel: OfflineModelRecord?
    ): List<SpeechWindow> {
        val modelFile = vadModel?.installedPath?.let(::File)
        if (modelFile?.isFile == true) {
            runCatching { detectSpeechWindowsWithSilero(pcmFile, modelFile) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        return detectSpeechWindowsByAmplitude(pcmFile)
    }

    private suspend fun detectSpeechWindowsWithSilero(pcmFile: File, modelFile: File): List<SpeechWindow> {
        val vad = Vad(
            null,
            VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = modelFile.absolutePath,
                    threshold = 0.25f,
                    minSilenceDuration = 0.8f,
                    minSpeechDuration = 0.25f,
                    windowSize = VAD_WINDOW_SAMPLES,
                    maxSpeechDuration = 30f
                ),
                sampleRate = TARGET_SAMPLE_RATE,
                numThreads = 1,
                provider = "cpu",
                debug = false
            )
        )
        return try {
            FileInputStream(pcmFile).use { input ->
                val bytes = ByteArray(VAD_WINDOW_SAMPLES * 2)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(bytes)
                    if (read <= 0) break
                    val samples = FloatArray((read / 2).coerceAtLeast(VAD_WINDOW_SAMPLES))
                    var index = 0
                    while (index < read / 2) {
                        val lo = bytes[index * 2].toInt() and 0xff
                        val hi = bytes[index * 2 + 1].toInt()
                        samples[index] = ((hi shl 8) or lo).toShort() / Short.MAX_VALUE.toFloat()
                        index++
                    }
                    vad.acceptWaveform(samples)
                }
            }
            vad.flush()
            buildList {
                while (!vad.empty()) {
                    val segment = vad.front()
                    val startMs = segment.start * 1000L / TARGET_SAMPLE_RATE
                    val endMs = (segment.start + segment.samples.size) * 1000L / TARGET_SAMPLE_RATE
                    add(
                        SpeechWindow(
                            startMs = (startMs - VAD_PRE_ROLL_MS).coerceAtLeast(0L),
                            endMs = (endMs + VAD_POST_ROLL_MS).coerceAtMost(pcmDurationMs(pcmFile))
                        )
                    )
                    vad.pop()
                }
            }.mergeCloseWindows(maxGapMs = VAD_MERGE_GAP_MS)
        } finally {
            vad.release()
        }
    }

    private suspend fun detectSpeechWindowsByAmplitude(pcmFile: File): List<SpeechWindow> {
        val windows = mutableListOf<SpeechWindow>()
        FileInputStream(pcmFile).use { input ->
            val buffer = ByteArray(TARGET_SAMPLE_RATE / 10 * 2)
            var frameOffset = 0L
            var currentStart: Long? = null
            var lastSpeechEndMs: Long? = null
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                val sampleCount = read / 2
                var peak = 0
                var index = 0
                while (index < sampleCount) {
                    val lo = buffer[index * 2].toInt() and 0xff
                    val hi = buffer[index * 2 + 1].toInt()
                    val sample = (hi shl 8) or lo
                    peak = maxOf(peak, abs(sample))
                    index++
                }
                val chunkStartMs = (frameOffset * 1000L) / TARGET_SAMPLE_RATE
                val chunkEndMs = ((frameOffset + sampleCount) * 1000L) / TARGET_SAMPLE_RATE
                if (peak >= SPEECH_PEAK_THRESHOLD) {
                    if (currentStart == null) {
                        currentStart = (chunkStartMs - AMPLITUDE_PRE_ROLL_MS).coerceAtLeast(0L)
                    }
                    lastSpeechEndMs = chunkEndMs
                } else if (currentStart != null && lastSpeechEndMs != null &&
                    chunkEndMs - lastSpeechEndMs >= AMPLITUDE_SILENCE_HANGOVER_MS
                ) {
                    windows += SpeechWindow(
                        startMs = currentStart,
                        endMs = (lastSpeechEndMs + AMPLITUDE_POST_ROLL_MS).coerceAtMost(chunkEndMs)
                    )
                    currentStart = null
                    lastSpeechEndMs = null
                }
                frameOffset += sampleCount
            }
            currentStart?.let { start ->
                val fileEndMs = (frameOffset * 1000L) / TARGET_SAMPLE_RATE
                windows += SpeechWindow(start, (lastSpeechEndMs ?: fileEndMs).coerceAtMost(fileEndMs))
            }
        }
        return windows.mergeCloseWindows(maxGapMs = AMPLITUDE_MERGE_GAP_MS)
    }

    private fun List<SpeechWindow>.mergeCloseWindows(maxGapMs: Long): List<SpeechWindow> {
        if (isEmpty()) return emptyList()
        val merged = mutableListOf(first())
        drop(1).forEach { next ->
            val previous = merged.removeAt(merged.lastIndex)
            if (next.startMs - previous.endMs <= maxGapMs) {
                merged += previous.copy(endMs = next.endMs)
            } else {
                merged += previous
                merged += next
            }
        }
        return merged
    }

    private fun parseWavHeader(file: File): WavHeader {
        RandomAccessFile(file, "r").use { input ->
            val riff = ByteArray(4)
            input.readFully(riff)
            if (!riff.contentEquals("RIFF".toByteArray())) throw EOFException()
            input.skipBytes(4)
            val wave = ByteArray(4)
            input.readFully(wave)
            if (!wave.contentEquals("WAVE".toByteArray())) throw EOFException()

            var sampleRate = 0
            var channelCount = 0
            var bitsPerSample = 0
            var dataOffset = -1L
            var dataSize = 0

            while (input.filePointer < input.length()) {
                val chunkId = ByteArray(4)
                input.readFully(chunkId)
                val chunkSize = Integer.reverseBytes(input.readInt())
                when (String(chunkId)) {
                    "fmt " -> {
                        val audioFormat = java.lang.Short.reverseBytes(input.readShort()).toInt()
                        channelCount = java.lang.Short.reverseBytes(input.readShort()).toInt()
                        sampleRate = Integer.reverseBytes(input.readInt())
                        input.skipBytes(6)
                        bitsPerSample = java.lang.Short.reverseBytes(input.readShort()).toInt()
                        if (chunkSize > 16) input.skipBytes(chunkSize - 16)
                        if (audioFormat != 1 || bitsPerSample != 16) throw OfflineAiException.FileCorrupt
                    }
                    "data" -> {
                        dataOffset = input.filePointer
                        dataSize = chunkSize
                        input.skipBytes(chunkSize)
                    }
                    else -> input.skipBytes(chunkSize)
                }
                if ((chunkSize and 1) == 1) input.skipBytes(1)
            }
            if (sampleRate <= 0 || channelCount <= 0 || bitsPerSample != 16 || dataOffset < 0 || dataSize <= 0) {
                throw OfflineAiException.FileCorrupt
            }
            return WavHeader(sampleRate, channelCount, dataOffset, dataSize)
        }
    }

    private fun readMonoPcm16(buffer: ByteArray, offset: Int, channelCount: Int): Short {
        var total = 0
        repeat(channelCount) { channel ->
            val sampleOffset = offset + channel * 2
            val lo = buffer[sampleOffset].toInt() and 0xff
            val hi = buffer[sampleOffset + 1].toInt()
            total += (hi shl 8) or lo
        }
        return (total.toDouble() / channelCount.toDouble()).roundToInt().toShort()
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

    private fun writeLittleEndianShort(output: FileOutputStream, sample: Short) {
        output.write(sample.toInt() and 0xff)
        output.write((sample.toInt() shr 8) and 0xff)
    }

    private suspend fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private data class WavHeader(
        val sampleRate: Int,
        val channelCount: Int,
        val dataOffset: Long,
        val dataSize: Int
    )

    private data class ResampleWriteResult(
        val sourceFramesRead: Long,
        val nextOutputFrame: Long
    )

    private companion object {
        const val TARGET_SAMPLE_RATE = 16_000
        const val SPEECH_PEAK_THRESHOLD = 512
        const val VAD_WINDOW_SAMPLES = 512
        const val VAD_PRE_ROLL_MS = 300L
        const val VAD_POST_ROLL_MS = 700L
        const val VAD_MERGE_GAP_MS = 800L
        const val AMPLITUDE_MERGE_GAP_MS = 900L
        const val AMPLITUDE_SILENCE_HANGOVER_MS = 800L
        const val AMPLITUDE_PRE_ROLL_MS = 250L
        const val AMPLITUDE_POST_ROLL_MS = 350L
        const val BUFFER_TIMEOUT_US = 10_000L
    }

    private fun pcmDurationMs(file: File): Long = file.length() * 1000L / (TARGET_SAMPLE_RATE * 2L)
}

data class PreparedTranscriptionAudio(
    val sourceFile: File,
    val sourceFingerprint: String,
    val pcm16kMonoFile: File,
    val speechWindows: List<SpeechWindow>,
    val decodeAndResampleMs: Long = 0,
    val speechDetectionMs: Long = 0
)
