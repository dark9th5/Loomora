package com.loomora.core.audio.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.loomora.core.audio.model.RecorderErrorType
import com.loomora.core.audio.model.RecordingStopResult
import com.loomora.core.audio.model.RecorderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecordEngine @Inject constructor() {

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var currentRecordingId: String? = null

    private val durationTracker = RecordingDurationTracker()

    private var timerJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startRecording(context: Context, recordingId: String, outputFile: File): Boolean {
        if (_state.value is RecorderState.Recording || _state.value is RecorderState.Paused) {
            return false
        }

        _state.value = RecorderState.Preparing

        currentRecordingId = recordingId
        outputFile.parentFile?.mkdirs()
        currentOutputFile = outputFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setAudioChannels(2)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            durationTracker.start()

            _state.value = RecorderState.Recording(recordingId, 0L)
            startTimerAndAmplitudeSampling()

            return true
        } catch (e: Exception) {
            releaseRecorder()
            currentOutputFile?.delete()
            _state.value = RecorderState.Error(
                type = RecorderErrorType.START_FAILED,
                message = RecorderErrorType.START_FAILED.name,
                recordingId = recordingId
            )
            currentRecordingId = null
            currentOutputFile = null
            durationTracker.reset()
            return false
        }
    }

    fun pauseRecording() {
        val currentState = _state.value
        if (currentState is RecorderState.Recording && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                val currentDuration = durationTracker.pause()
                _state.value = RecorderState.Paused(currentState.recordingId, currentDuration)
            } catch (e: Exception) {
                _state.value = RecorderState.Error(
                    type = RecorderErrorType.PAUSE_FAILED,
                    message = RecorderErrorType.PAUSE_FAILED.name,
                    recordingId = currentState.recordingId
                )
            }
        }
    }

    fun resumeRecording() {
        val currentState = _state.value
        if (currentState is RecorderState.Paused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                val currentDuration = durationTracker.resume()
                _state.value = RecorderState.Recording(currentState.recordingId, currentDuration)
            } catch (e: Exception) {
                _state.value = RecorderState.Error(
                    type = RecorderErrorType.RESUME_FAILED,
                    message = RecorderErrorType.RESUME_FAILED.name,
                    recordingId = currentState.recordingId
                )
            }
        }
    }

    fun stopRecording(): RecordingStopResult? {
        val currentState = _state.value
        if (currentState !is RecorderState.Recording && currentState !is RecorderState.Paused) {
            return null
        }

        val recId = when (currentState) {
            is RecorderState.Recording -> currentState.recordingId
            is RecorderState.Paused -> currentState.recordingId
            else -> requireNotNull(currentRecordingId)
        }
        val finalDurationMs = when (currentState) {
            is RecorderState.Recording -> durationTracker.elapsed()
            is RecorderState.Paused -> currentState.durationMs
            else -> durationTracker.elapsed()
        }

        _state.value = RecorderState.Finalizing(recId, finalDurationMs)
        cancelTimerAndAmplitudeSampling()

        val file = currentOutputFile

        try {
            mediaRecorder?.stop()
            releaseRecorder()

            if (file != null && file.exists() && file.length() > 0) {
                _state.value = RecorderState.Saving(recId, file.absolutePath, finalDurationMs)
                return RecordingStopResult(recId, file, finalDurationMs)
            } else {
                _state.value = RecorderState.Error(
                    type = RecorderErrorType.FINALIZE_FAILED,
                    message = RecorderErrorType.FINALIZE_FAILED.name,
                    recordingId = recId,
                    safeSavedPath = file?.absolutePath
                )
            }
        } catch (e: Exception) {
            releaseRecorder()
            _state.value = RecorderState.Error(
                type = RecorderErrorType.FINALIZE_FAILED,
                message = RecorderErrorType.FINALIZE_FAILED.name,
                recordingId = recId,
                safeSavedPath = file?.absolutePath
            )
        }

        return null
    }

    fun markSaved(recordingId: String, fileUri: String, durationMs: Long) {
        cancelTimerAndAmplitudeSampling()
        _state.value = RecorderState.Saved(recordingId, fileUri, durationMs)
        currentRecordingId = null
        currentOutputFile = null
        durationTracker.reset()
    }

    fun markSaveFailed(recordingId: String, fileUri: String, durationMs: Long) {
        cancelTimerAndAmplitudeSampling()
        _state.value = RecorderState.Error(
            type = RecorderErrorType.SAVE_FAILED,
            message = RecorderErrorType.SAVE_FAILED.name,
            recordingId = recordingId,
            safeSavedPath = fileUri
        )
    }

    fun release() {
        cancelTimerAndAmplitudeSampling()
        releaseRecorder()
        currentRecordingId = null
        currentOutputFile = null
        durationTracker.reset()
        _amplitude.value = 0f
        if (_state.value !is RecorderState.Saved) {
            _state.value = RecorderState.Idle
        }
    }

    private fun startTimerAndAmplitudeSampling() {
        timerJob?.cancel()
        timerJob = engineScope.launch {
            while (isActive) {
                val currentState = _state.value
                if (currentState is RecorderState.Recording) {
                    val duration = calculateElapsedDurationMs()
                    _state.value = RecorderState.Recording(currentState.recordingId, duration)

                    // Real RMS/Peak amplitude sampling from MediaRecorder
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)
                    _amplitude.value = normalizedAmp
                }
                delay(100)
            }
        }
    }

    private fun calculateElapsedDurationMs(): Long {
        return durationTracker.elapsed(isPaused = _state.value is RecorderState.Paused)
    }

    fun getCurrentDurationMs(): Long {
        return calculateElapsedDurationMs()
    }

    private fun cancelTimerAndAmplitudeSampling() {
        timerJob?.cancel()
        timerJob = null
        _amplitude.value = 0f
    }

    private fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}

internal class RecordingDurationTracker(
    private val nowMs: () -> Long = System::currentTimeMillis
) {
    private var startTimeMs: Long = 0L
    private var totalPausedMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L
    private var pausedDurationMs: Long = 0L
    private var isPaused: Boolean = false
    private var hasStarted: Boolean = false

    fun start() {
        startTimeMs = nowMs()
        totalPausedMs = 0L
        pauseStartTimeMs = 0L
        pausedDurationMs = 0L
        isPaused = false
        hasStarted = true
    }

    fun pause(): Long {
        pauseStartTimeMs = nowMs()
        pausedDurationMs = elapsed(isPaused = false)
        isPaused = true
        return pausedDurationMs
    }

    fun resume(): Long {
        if (isPaused) {
            totalPausedMs += (nowMs() - pauseStartTimeMs).coerceAtLeast(0L)
        }
        isPaused = false
        return elapsed(isPaused = false)
    }

    fun elapsed(isPaused: Boolean = this.isPaused): Long {
        if (!hasStarted) {
            return 0L
        }
        return if (isPaused) {
            pausedDurationMs
        } else {
            (nowMs() - startTimeMs - totalPausedMs).coerceAtLeast(0L)
        }
    }

    fun reset() {
        startTimeMs = 0L
        totalPausedMs = 0L
        pauseStartTimeMs = 0L
        pausedDurationMs = 0L
        isPaused = false
        hasStarted = false
    }
}
