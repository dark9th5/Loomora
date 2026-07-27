package com.loomora.core.audio

import com.loomora.core.audio.model.RecorderState
import org.junit.Assert.assertEquals
import org.junit.Test

class RecorderStateTest {

    @Test
    fun recorderState_recordingHoldsSessionAndDuration() {
        val state = RecorderState.Recording(recordingId = "rec-1", durationMs = 15000L)
        assertEquals("rec-1", state.recordingId)
        assertEquals(15000L, state.durationMs)
    }

    @Test
    fun recorderState_errorCanCarrySafeSavedPath() {
        val state = RecorderState.Error(
            type = com.loomora.core.audio.model.RecorderErrorType.SAVE_FAILED,
            message = "Microphone interrupted",
            safeSavedPath = "/tmp/rec.m4a"
        )
        assertEquals("/tmp/rec.m4a", state.safeSavedPath)
    }

    @Test
    fun recorderState_savedCarriesFinalDuration() {
        val state = RecorderState.Saved(recordingId = "rec-1", fileUri = "/tmp/rec.m4a", durationMs = 42L)
        assertEquals("rec-1", state.recordingId)
        assertEquals(42L, state.durationMs)
    }
}
