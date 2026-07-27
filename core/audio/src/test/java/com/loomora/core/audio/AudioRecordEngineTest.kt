package com.loomora.core.audio

import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.audio.engine.RecordingDurationTracker
import com.loomora.core.audio.model.RecorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecordEngineTest {

    @Test
    fun initialState_isIdle() {
        val engine = AudioRecordEngine()
        assertEquals(RecorderState.Idle, engine.state.value)
    }

    @Test
    fun amplitude_initialValueIsZero() {
        val engine = AudioRecordEngine()
        assertEquals(0f, engine.amplitude.value, 0.001f)
    }

    @Test
    fun stopRecordingWhenIdle_returnsNull() {
        val engine = AudioRecordEngine()
        val file = engine.stopRecording()
        assertTrue(file == null)
    }

    @Test
    fun releaseWhenIdle_isIdempotentAndResetsTransientState() {
        val engine = AudioRecordEngine()

        engine.release()
        engine.release()

        assertEquals(RecorderState.Idle, engine.state.value)
        assertEquals(0f, engine.amplitude.value, 0.001f)
        assertEquals(0L, engine.getCurrentDurationMs())
    }

    @Test
    fun durationTracker_stopWhilePaused_excludesPausedTime() {
        var now = 1_000L
        val tracker = RecordingDurationTracker { now }

        tracker.start()
        now = 6_000L
        val pausedAt = tracker.pause()
        now = 16_000L

        assertEquals(5_000L, pausedAt)
        assertEquals(5_000L, tracker.elapsed())
    }

    @Test
    fun durationTracker_multiplePauses_excludesAllPausedTime() {
        var now = 0L
        val tracker = RecordingDurationTracker { now }

        tracker.start()
        now = 5_000L
        tracker.pause()
        now = 8_000L
        tracker.resume()
        now = 12_000L
        tracker.pause()
        now = 22_000L
        tracker.resume()
        now = 25_000L

        assertEquals(12_000L, tracker.elapsed())
    }
}
