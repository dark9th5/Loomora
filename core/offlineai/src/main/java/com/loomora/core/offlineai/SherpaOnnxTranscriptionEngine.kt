package com.loomora.core.offlineai

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.loomora.core.model.TranscriptSegment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong

class SherpaOnnxTranscriptionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalTranscriptionEngine {
    private val inferenceMutex = Mutex()
    private val recognizerCache = SingleEntryResourceCache<RecognizerCacheKey, OfflineRecognizer> { it.release() }

    override suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput = inferenceMutex.withLock {
        withContext(Dispatchers.IO) {
        val installedPath = input.model.installedPath ?: throw OfflineAiException.ModelFileMissing
        val modelFile = File(installedPath)
        if (!modelFile.exists() || !modelFile.isFile) {
            throw OfflineAiException.ModelFileMissing
        }
        if (!input.pcm16kMonoFile.exists() || input.pcm16kMonoFile.length() <= 0L) {
            return@withContext TranscriptionOutput(
                segments = emptyList(),
                modelId = input.model.manifest.id,
                modelVersion = input.model.manifest.version,
                languageTag = input.languageHint,
                processingDurationMs = 0L,
                memoryObservationKb = currentUsedMemoryKb()
            )
        }

        val modelDir = modelFile.parentFile ?: throw OfflineAiException.ModelFileMissing
        val encoder = findModelFile(modelDir, preferred = modelFile, contains = "encoder")
            ?: throw OfflineAiException.ModelFileMissing
        val decoder = findModelFile(modelDir, preferred = null, contains = "decoder")
            ?: throw OfflineAiException.ModelFileMissing
        val joiner = findModelFile(modelDir, preferred = null, contains = "joiner")
        val tokens = findModelAsset(modelDir, contains = "tokens")
            ?: throw OfflineAiException.ModelFileMissing
        val cacheKey = RecognizerCacheKey(
            modelId = input.model.manifest.id,
            modelVersion = input.model.manifest.version,
            encoderPath = encoder.absolutePath,
            decoderPath = decoder.absolutePath,
            joinerPath = joiner?.absolutePath,
            tokensPath = tokens.absolutePath,
            language = normalizeWhisperLanguage(input.languageHint),
            threadCount = input.performanceProfile.threadCount
        )
        val recognizer = recognizerFor(
            key = cacheKey,
            encoder = encoder,
            decoder = decoder,
            joiner = joiner,
            tokens = tokens,
            languageHint = input.languageHint,
            threadCount = input.performanceProfile.threadCount
        )

        val startedAt = System.currentTimeMillis()
        try {
            val windows = input.speechWindows
                .filter { it.endMs > it.startMs }
                .ifEmpty { listOf(SpeechWindow(0L, pcmDurationMs(input.pcm16kMonoFile))) }
            var detectedLanguage = input.languageHint ?: if (joiner != null) "vi" else null
            val segments = windows.flatMap { window ->
                coroutineContext.ensureActive()
                val stream = recognizer.createStream()
                try {
                    feedPcm16kMonoWindow(input.pcm16kMonoFile, window, stream::acceptWaveform)
                    recognizer.decode(stream)
                    val result = recognizer.getResult(stream)
                    if (result.lang.isNotBlank()) detectedLanguage = result.lang
                    resultToSegments(
                        result.text,
                        result.timestamps,
                        result.durations,
                        window.endMs - window.startMs
                    ).map { segment ->
                        segment.copy(
                            startMs = segment.startMs + window.startMs,
                            endMs = segment.endMs + window.startMs
                        )
                    }
                } finally {
                    stream.release()
                }
            }
                return@withContext TranscriptionOutput(
                    segments = segments,
                    modelId = input.model.manifest.id,
                    modelVersion = input.model.manifest.version,
                    languageTag = detectedLanguage,
                    processingDurationMs = System.currentTimeMillis() - startedAt,
                    memoryObservationKb = currentUsedMemoryKb()
                )
        } catch (error: UnsatisfiedLinkError) {
            throw OfflineAiException.ModelInitializationFailed
        } catch (error: NoClassDefFoundError) {
            throw OfflineAiException.ProcessingUnavailable
        } catch (error: OfflineAiException) {
            throw error
        } catch (_: Exception) {
            throw OfflineAiException.ModelInitializationFailed
        }
    } }

    override fun close() {
        recognizerCache.clear()
    }

    private fun recognizerFor(
        key: RecognizerCacheKey,
        encoder: File,
        decoder: File,
        joiner: File?,
        tokens: File,
        languageHint: String?,
        threadCount: Int
    ): OfflineRecognizer = recognizerCache.getOrCreate(key) {
        createRecognizer(encoder, decoder, joiner, tokens, languageHint, threadCount)
    }

    private fun createRecognizer(
        encoder: File,
        decoder: File,
        joiner: File?,
        tokens: File,
        languageHint: String?,
        threadCount: Int
    ): OfflineRecognizer {
        val modelConfig = if (joiner != null) {
            OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = encoder.absolutePath,
                    decoder = decoder.absolutePath,
                    joiner = joiner.absolutePath
                ),
                numThreads = threadCount,
                debug = false,
                provider = "cpu",
                tokens = tokens.absolutePath
            )
        } else {
            OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = encoder.absolutePath,
                    decoder = decoder.absolutePath,
                    language = normalizeWhisperLanguage(languageHint),
                    task = "transcribe",
                    tailPaddings = -1,
                    enableTokenTimestamps = true,
                    enableSegmentTimestamps = true
                ),
                numThreads = threadCount,
                debug = false,
                provider = "cpu",
                tokens = tokens.absolutePath
            )
        }
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
            modelConfig = modelConfig,
            decodingMethod = "greedy_search"
        )
        return OfflineRecognizer(null, config)
    }

    private fun findModelFile(
        modelDir: File,
        preferred: File?,
        contains: String
    ): File? {
        if (preferred != null && preferred.name.contains(contains, ignoreCase = true)) {
            return preferred
        }
        return modelDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
            .firstOrNull { it.name.contains(contains, ignoreCase = true) }
    }

    private fun findModelAsset(
        modelDir: File,
        contains: String
    ): File? {
        return modelDir.walkTopDown()
            .filter { it.isFile }
            .firstOrNull { it.name.contains(contains, ignoreCase = true) }
    }

    private suspend fun feedPcm16kMonoWindow(
        pcmFile: File,
        window: SpeechWindow,
        acceptWaveform: (FloatArray, Int) -> Unit
    ) {
        val startByte = (window.startMs * BYTES_PER_SECOND / 1000L).coerceAtLeast(0L)
        var remaining = ((window.endMs - window.startMs) * BYTES_PER_SECOND / 1000L)
            .coerceAtMost((pcmFile.length() - startByte).coerceAtLeast(0L))
        RandomAccessFile(pcmFile, "r").use { input ->
            input.seek(startByte.coerceAtMost(input.length()))
            val pcm = ByteArray(CHUNK_SAMPLES * 2)
            while (remaining > 0L) {
                coroutineContext.ensureActive()
                val read = input.read(pcm, 0, minOf(pcm.size.toLong(), remaining).toInt())
                if (read <= 0) break
                val sampleBytes = read - (read % 2)
                val floats = FloatArray(sampleBytes / 2) { index ->
                    val lo = pcm[index * 2].toInt() and 0xff
                    val hi = pcm[index * 2 + 1].toInt()
                    (((hi shl 8) or lo).toShort().toFloat() / Short.MAX_VALUE.toFloat())
                }
                if (floats.isNotEmpty()) acceptWaveform(floats, SAMPLE_RATE)
                remaining -= read
            }
        }
    }

    private fun pcmDurationMs(file: File): Long = file.length() * 1000L / BYTES_PER_SECOND

    private fun resultToSegments(
        text: String,
        timestamps: FloatArray,
        durations: FloatArray,
        fallbackDurationMs: Long
    ): List<TranscriptSegment> {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return emptyList()
        val startMs = timestamps.firstOrNull()?.let { (it * 1000f).roundToLong() } ?: 0L
        val endMs = when {
            timestamps.isNotEmpty() && durations.isNotEmpty() -> {
                ((timestamps.last() + durations.last()) * 1000f).roundToLong()
            }
            timestamps.isNotEmpty() -> (timestamps.last() * 1000f).roundToLong().coerceAtLeast(startMs + 1L)
            else -> startMs + 1L
        }
        val timestampSpan = endMs - startMs
        val useWindowBounds = fallbackDurationMs >= 1_000L && timestampSpan < minOf(500L, fallbackDurationMs / 4L)
        return TranscriptTextSegmenter.segment(
            text = normalizedText,
            startMs = if (useWindowBounds) 0L else startMs,
            endMs = if (useWindowBounds) fallbackDurationMs else endMs.coerceAtLeast(startMs + 1L)
        )
    }

    private fun normalizeWhisperLanguage(languageHint: String?): String {
        val hint = languageHint?.lowercase()?.substringBefore("-")
        return when (hint) {
            "vi", "vie", "vietnamese" -> "vi"
            "en", "eng", "english" -> "en"
            else -> ""
        }
    }

    private fun currentUsedMemoryKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L
    }

    private companion object {
        const val CHUNK_SAMPLES = 16_000
        const val SAMPLE_RATE = 16_000
        const val BYTES_PER_SECOND = SAMPLE_RATE * 2L
    }

    private data class RecognizerCacheKey(
        val modelId: String,
        val modelVersion: String,
        val encoderPath: String,
        val decoderPath: String,
        val joinerPath: String?,
        val tokensPath: String,
        val language: String,
        val threadCount: Int
    )
}
