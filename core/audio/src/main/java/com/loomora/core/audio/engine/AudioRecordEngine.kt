package com.loomora.core.audio.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.loomora.core.audio.model.RecorderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
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

    private var startTimeMs: Long = 0L
    private var totalPausedMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L

    private var timerJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + Job())

    fun startRecording(context: Context, title: String): String {
        if (_state.value is RecorderState.Recording || _state.value is RecorderState.Paused) {
            return currentRecordingId ?: ""
        }

        _state.value = RecorderState.Preparing

        val id = UUID.randomUUID().toString()
        currentRecordingId = id

        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        val file = File(recordingsDir, "$id.m4a")
        currentOutputFile = file

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
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            totalPausedMs = 0L

            _state.value = RecorderState.Recording(0L)
            startTimerAndAmplitudeSampling()

            return id
        } catch (e: Exception) {
            currentOutputFile?.delete()
            _state.value = RecorderState.Error("Failed to initialize audio recorder: ${e.localizedMessage}")
            return ""
        }
    }

    fun pauseRecording() {
        val currentState = _state.value
        if (currentState is RecorderState.Recording && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                pauseStartTimeMs = System.currentTimeMillis()
                val currentDuration = calculateElapsedDurationMs()
                _state.value = RecorderState.Paused(currentDuration)
            } catch (e: Exception) {
                _state.value = RecorderState.Error("Failed to pause recording: ${e.localizedMessage}")
            }
        }
    }

    fun resumeRecording() {
        val currentState = _state.value
        if (currentState is RecorderState.Paused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                totalPausedMs += (System.currentTimeMillis() - pauseStartTimeMs)
                val currentDuration = calculateElapsedDurationMs()
                _state.value = RecorderState.Recording(currentDuration)
            } catch (e: Exception) {
                _state.value = RecorderState.Error("Failed to resume recording: ${e.localizedMessage}")
            }
        }
    }

    fun stopRecording(): File? {
        val currentState = _state.value
        if (currentState !is RecorderState.Recording && currentState !is RecorderState.Paused) {
            return null
        }

        _state.value = RecorderState.Finalizing
        timerJob?.cancel()

        val file = currentOutputFile
        val recId = currentRecordingId ?: ""

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            if (file != null && file.exists() && file.length() > 0) {
                _state.value = RecorderState.Completed(recId, file.absolutePath)
            } else {
                _state.value = RecorderState.Error("Recording file is empty or missing")
            }
        } catch (e: Exception) {
            _state.value = RecorderState.Error("Error finalizing recording file: ${e.localizedMessage}")
        }

        return file
    }

    private fun startTimerAndAmplitudeSampling() {
        timerJob?.cancel()
        timerJob = engineScope.launch {
            while (isActive) {
                val currentState = _state.value
                if (currentState is RecorderState.Recording) {
                    val duration = calculateElapsedDurationMs()
                    _state.value = RecorderState.Recording(duration)

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
        return (System.currentTimeMillis() - startTimeMs - totalPausedMs).coerceAtLeast(0L)
    }

    fun getCurrentDurationMs(): Long {
        return calculateElapsedDurationMs()
    }
}
