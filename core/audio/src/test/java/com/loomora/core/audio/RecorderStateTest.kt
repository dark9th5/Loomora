package com.loomora.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorderStateTest {

    @Test
    fun recorderState_recordingHoldsDurationAndAmplitude() {
        val state = RecorderState.Recording(durationMs = 15000L, currentAmplitude = 0.75f)
        assertEquals(15000L, state.durationMs)
        assertEquals(0.75f, state.currentAmplitude, 0.001f)
    }

    @Test
    fun recorderState_errorDistinguishesRecoverable() {
        val state = RecorderState.Error(message = "Microphone interrupted", isRecoverable = true)
        assertTrue(state.isRecoverable)
    }
}
