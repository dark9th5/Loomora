package com.loomora.core.audio.waveform

import com.loomora.core.model.Recording
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class WaveformRepository @Inject constructor(
    private val cacheStore: WaveformCacheStore,
    private val wavDecoder: WavAudioWaveformDecoder,
    private val androidDecoder: AndroidAudioWaveformDecoder
) {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun loadWaveform(
        recording: Recording,
        resolution: Int
    ): Flow<WaveformLoadState> = flow {
        val sourceFile = recording.sourceFileOrNull()
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile) {
            emit(WaveformLoadState.Error(WaveformErrorCode.SOURCE_MISSING))
            return@flow
        }

        emit(WaveformLoadState.Loading)
        val fingerprint = cacheStore.computeSourceFingerprint(sourceFile)
        val cacheKey = cacheStore.cacheKey(
            sourceFingerprint = fingerprint,
            algorithmVersion = WaveformAlgorithm.VERSION,
            resolution = resolution
        )
        cacheStore.read(cacheKey)?.let { cached ->
            emit(WaveformLoadState.Ready(cached))
            return@flow
        }

        activeJobs[recording.id] = coroutineContext[Job]
            ?: throw IllegalStateException("Waveform flow is missing a Job")
        try {
            coroutineContext.ensureActive()
            val decoder = selectDecoder(sourceFile)
                ?: run {
                    emit(WaveformLoadState.Error(WaveformErrorCode.UNSUPPORTED_FORMAT))
                    return@flow
                }
            val decoded = decoder.decode(sourceFile, resolution)
                .getOrElse { throwable ->
                    emit(
                        WaveformLoadState.Error(
                            if (throwable is IllegalArgumentException) {
                                WaveformErrorCode.CORRUPT_INPUT
                            } else {
                                WaveformErrorCode.DECODE_FAILED
                            }
                        )
                    )
                    return@flow
                }
            val waveform = decoded.copy(sourceFingerprint = fingerprint)
            cacheStore.write(cacheKey, waveform)
            emit(WaveformLoadState.Ready(waveform))
        } catch (_: CancellationException) {
            throw CancellationException()
        } catch (_: IllegalStateException) {
            emit(WaveformLoadState.Error(WaveformErrorCode.CACHE_IO))
        } finally {
            activeJobs.remove(recording.id)
        }
    }.flowOn(ioDispatcher)

    fun cancelGeneration(recordingId: String) {
        activeJobs.remove(recordingId)?.cancel()
    }

    fun invalidate(recording: Recording) {
        recording.sourceFileOrNull()
            ?.takeIf { it.exists() && it.isFile }
            ?.let(cacheStore::computeSourceFingerprint)
            ?.let(cacheStore::removeForFingerprint)
    }

    private fun selectDecoder(sourceFile: File): AudioWaveformDecoder? {
        return when {
            wavDecoder.canDecode(sourceFile) -> wavDecoder
            androidDecoder.canDecode(sourceFile) -> androidDecoder
            else -> null
        }
    }

    private fun Recording.sourceFileOrNull(): File? {
        val path = originalFileUri.removePrefix("file://")
        if (path.isBlank() || path.contains("..")) {
            return null
        }
        return File(path)
    }
}
