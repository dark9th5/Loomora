package com.loomora.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingTest {

    @Test
    fun recording_defaultValuesAreValid() {
        val recording = Recording(
            id = "rec-1",
            title = "Test Note",
            createdAt = 1000L,
            updatedAt = 1000L,
            durationMs = 60000L,
            status = RecordingStatus.SAVED,
            originalFileUri = "file:///path/to/file.aac"
        )

        assertEquals("rec-1", recording.id)
        assertEquals("Test Note", recording.title)
        assertEquals(RecordingStatus.SAVED, recording.status)
        assertEquals("file:///path/to/file.aac", recording.originalFileUri)
    }
}
