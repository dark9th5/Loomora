package com.loomora.core.offlineai

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.loomora.core.model.TranscriptSegment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong

class SherpaOnnxTranscriptionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalTranscriptionEngine {
    override suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput = withContext(Dispatchers.IO) {
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
        val tokens = findModelAsset(modelDir, contains = "tokens")
            ?: throw OfflineAiException.ModelFileMissing
        val recognizer = createRecognizer(
            encoder = encoder,
            decoder = decoder,
            tokens = tokens,
            languageHint = input.languageHint
        )

        val startedAt = System.currentTimeMillis()
        try {
            val stream = recognizer.createStream()
            try {
                feedPcm16kMono(input.pcm16kMonoFile, stream::acceptWaveform)
                recognizer.decode(stream)
                val result = recognizer.getResult(stream)
                val segments = resultToSegments(result.text, result.timestamps, result.durations)
                return@withContext TranscriptionOutput(
                    segments = segments,
                    modelId = input.model.manifest.id,
                    modelVersion = input.model.manifest.version,
                    languageTag = result.lang.ifBlank { input.languageHint },
                    processingDurationMs = System.currentTimeMillis() - startedAt,
                    memoryObservationKb = currentUsedMemoryKb()
                )
            } finally {
                stream.release()
            }
        } catch (error: UnsatisfiedLinkError) {
            throw OfflineAiException.ModelInitializationFailed
        } catch (error: NoClassDefFoundError) {
            throw OfflineAiException.ProcessingUnavailable
        } catch (error: OfflineAiException) {
            throw error
        } catch (_: Exception) {
            throw OfflineAiException.ModelInitializationFailed
        } finally {
            recognizer.release()
        }
    }

    override fun close() = Unit

    private fun createRecognizer(
        encoder: File,
        decoder: File,
        tokens: File,
        languageHint: String?
    ): OfflineRecognizer {
        val whisper = OfflineWhisperModelConfig(
            encoder = encoder.absolutePath,
            decoder = decoder.absolutePath,
            language = normalizeWhisperLanguage(languageHint),
            task = "transcribe",
            tailPaddings = -1,
            enableTokenTimestamps = true,
            enableSegmentTimestamps = true
        )
        val modelConfig = OfflineModelConfig(
            whisper = whisper,
            numThreads = 2,
            debug = false,
            provider = "cpu",
            tokens = tokens.absolutePath
        )
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

    private suspend fun feedPcm16kMono(
        pcmFile: File,
        acceptWaveform: (FloatArray, Int) -> Unit
    ) {
        FileInputStream(pcmFile).use { input ->
            val pcm = ByteArray(CHUNK_SAMPLES * 2)
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(pcm)
                if (read <= 0) break
                val samples = read / 2
                val floats = FloatArray(samples)
                var index = 0
                while (index < samples) {
                    val lo = pcm[index * 2].toInt() and 0xff
                    val hi = pcm[index * 2 + 1].toInt()
                    val sample = ((hi shl 8) or lo).toShort()
                    floats[index] = sample.toFloat() / Short.MAX_VALUE.toFloat()
                    index++
                }
                acceptWaveform(floats, 16_000)
            }
        }
    }

    private fun resultToSegments(
        text: String,
        timestamps: FloatArray,
        durations: FloatArray
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
        return listOf(
            TranscriptSegment(
                startMs = startMs,
                endMs = endMs.coerceAtLeast(startMs + 1L),
                rawText = text,
                text = normalizedText
            )
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
    }
}
