package com.loomora.core.audio

import com.loomora.core.audio.engine.AudioRecordEngine
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
}
