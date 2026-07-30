package com.loomora.core.offlineai

import android.content.Context
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.loomora.core.model.SpeakerTurn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong

class SherpaOnnxDiarizationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalDiarizationEngine {
    override suspend fun diarize(input: DiarizationInput): DiarizationOutput = withContext(Dispatchers.IO) {
        val installedPath = input.model.installedPath ?: throw OfflineAiException.ModelFileMissing
        val installedFile = File(installedPath)
        if (!installedFile.exists()) throw OfflineAiException.ModelFileMissing
        val modelDir = installedFile.parentFile ?: throw OfflineAiException.ModelFileMissing
        val segmentation = findModelFile(modelDir, listOf("segmentation", "pyannote"))
            ?: throw OfflineAiException.ModelFileMissing
        val embedding = findModelFile(modelDir, listOf("3dspeaker", "3d-speaker", "embedding", "speaker"))
            ?: throw OfflineAiException.ModelFileMissing
        if (!input.pcm16kMonoFile.exists() || input.pcm16kMonoFile.length() <= 0L) {
            return@withContext DiarizationOutput(
                turns = emptyList(),
                modelId = input.model.manifest.id,
                modelVersion = input.model.manifest.version,
                clusteringSettings = input.clustering,
                processingDurationMs = 0L,
                memoryObservationKb = currentUsedMemoryKb()
            )
        }

        val diarizer = createDiarizer(segmentation, embedding, input.clustering)
        val startedAt = System.currentTimeMillis()
        try {
            val samples = readPcm16AsFloatArray(input.pcm16kMonoFile)
            coroutineContext.ensureActive()
            val result = diarizer.process(samples)
            coroutineContext.ensureActive()
            val normalizedSpeakerIndexes = result.map { it.speaker }.distinct().sorted()
                .withIndex().associate { (normalized, raw) -> raw to normalized }
            val turns = SpeakerTurnStabilizer.stabilize(
                result.map {
                    val speakerIndex = normalizedSpeakerIndexes.getValue(it.speaker)
                    SpeakerTurn(
                        startMs = (it.start * 1000f).roundToLong().coerceAtLeast(0L),
                        endMs = (it.end * 1000f).roundToLong().coerceAtLeast(0L),
                        speakerLabel = genericSpeakerLabel(speakerIndex),
                        speakerIndex = speakerIndex,
                        confidence = null,
                        isOverlapped = false,
                        isUncertain = false
                    )
                }
            )
            DiarizationOutput(
                turns = turns,
                modelId = input.model.manifest.id,
                modelVersion = input.model.manifest.version,
                clusteringSettings = input.clustering,
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
        } finally {
            diarizer.release()
        }
    }

    override fun close() = Unit

    private fun createDiarizer(
        segmentation: File,
        embedding: File,
        clustering: DiarizationClusteringSettings
    ): OfflineSpeakerDiarization {
        val segmentationConfig = OfflineSpeakerSegmentationModelConfig(
            OfflineSpeakerSegmentationPyannoteModelConfig(segmentation.absolutePath),
            clustering.numThreads,
            false,
            clustering.provider
        )
        val embeddingConfig = SpeakerEmbeddingExtractorConfig(
            embedding.absolutePath,
            clustering.numThreads,
            false,
            clustering.provider
        )
        val clusteringConfig = FastClusteringConfig(
            clustering.numClusters,
            clustering.threshold
        )
        val config = OfflineSpeakerDiarizationConfig(
            segmentationConfig,
            embeddingConfig,
            clusteringConfig,
            clustering.minDurationOnSec,
            clustering.minDurationOffSec
        )
        return OfflineSpeakerDiarization(null, config)
    }

    private suspend fun readPcm16AsFloatArray(pcmFile: File): FloatArray {
        val sampleCount = (pcmFile.length() / 2L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val samples = FloatArray(sampleCount)
        FileInputStream(pcmFile).use { input ->
            val buffer = ByteArray(CHUNK_SAMPLES * 2)
            var outputIndex = 0
            while (outputIndex < sampleCount) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                val count = read / 2
                var index = 0
                while (index < count && outputIndex < sampleCount) {
                    val lo = buffer[index * 2].toInt() and 0xff
                    val hi = buffer[index * 2 + 1].toInt()
                    val sample = ((hi shl 8) or lo).toShort()
                    samples[outputIndex] = sample.toFloat() / Short.MAX_VALUE.toFloat()
                    index++
                    outputIndex++
                }
            }
        }
        return samples
    }

    private fun findModelFile(modelDir: File, needles: List<String>): File? {
        return modelDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
            .firstOrNull { file ->
                val name = file.name.lowercase()
                needles.any { name.contains(it.lowercase()) }
            }
    }

    private fun genericSpeakerLabel(speakerIndex: Int): String = "Speaker ${speakerIndex + 1}"

    private fun currentUsedMemoryKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L
    }

    private companion object {
        const val CHUNK_SAMPLES = 16_000
    }
}
