package com.loomora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.database.entity.RecordingTaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RecordingTaskDaoTest {
    private lateinit var database: LoomoraDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LoomoraDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun generatedTask_survivesEditingCompletionAndArchive() = runTest {
        val now = 1_000L
        database.recordingDao().insertRecording(
            RecordingEntity(
                id = "recording-1",
                title = "Meeting",
                createdAt = now,
                updatedAt = now,
                durationMs = 60_000L,
                status = "SAVED",
                originalFileUri = "file:///tmp/meeting.m4a",
                mimeType = "audio/mp4",
                sampleRate = 48_000,
                channels = 1,
                bitrate = 128_000,
                sizeBytes = 1_024L
            )
        )
        val dao = database.recordingTaskDao()
        val task = RecordingTaskEntity(
            id = "task-1",
            recordingId = "recording-1",
            title = "Send the final report",
            assignee = "Lan",
            dueDate = "Friday",
            sourceGenerationMode = "HEURISTIC",
            createdAt = now,
            updatedAt = now
        )

        dao.insertGeneratedTasks(listOf(task))
        dao.updateContent("task-1", "Send the approved report", "Lan", "Monday", now + 1)
        dao.updateStatus("task-1", RecordingTaskEntity.STATUS_DONE, now + 2, now + 2)

        val completed = dao.observeTasksForRecording("recording-1").first().single()
        assertEquals("Send the approved report", completed.title)
        assertEquals("Monday", completed.dueDate)
        assertTrue(completed.isCompleted)
        assertTrue(completed.isUserEdited)

        dao.archive("task-1", now + 3)
        assertTrue(dao.observeTasksForRecording("recording-1").first().isEmpty())
    }
}
