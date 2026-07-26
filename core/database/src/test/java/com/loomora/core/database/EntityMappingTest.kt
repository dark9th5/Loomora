package com.loomora.core.database

import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappingTest {

    @Test
    fun recordingEntity_fieldsMappingIsCorrect() {
        val entity = RecordingEntity(
            id = "rec-123",
            title = "Sample Recording",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            durationMs = 120000L,
            status = "SAVED",
            originalFileUri = "file:///sdcard/loomora/rec-123.aac",
            editedOutputUri = null,
            mimeType = "audio/aac",
            sampleRate = 44100,
            channels = 2,
            bitrate = 128000,
            sizeBytes = 2048000L,
            languageHint = "en",
            isFavorite = true
        )

        assertEquals("rec-123", entity.id)
        assertEquals("Sample Recording", entity.title)
        assertEquals("SAVED", entity.status)
        assertEquals(true, entity.isFavorite)
    }
}
