package com.loomora.core.audio.recovery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.entity.RecordingEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecordingRecoveryScannerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: LoomoraDatabase
    private lateinit var validator: FakeRecordingFileValidator
    private lateinit var scanner: RecordingRecoveryScanner

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LoomoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        validator = FakeRecordingFileValidator()
        scanner = RecordingRecoveryScanner(context, database.recordingDao(), validator)
        recordingsDir().deleteRecursively()
        recordingsDir().mkdirs()
    }

    @After
    fun tearDown() {
        database.close()
        recordingsDir().deleteRecursively()
    }

    @Test
    fun interruptedRecordingWithMissingFile_isMarkedMissingWithoutSaved() = runBlocking {
        val recording = recording(id = "missing", status = "RECORDING", fileName = "missing.m4a")
        database.recordingDao().insertRecording(recording)

        scanner.scan()

        val recovered = database.recordingDao().getRecordingByIdSync("missing")
        assertEquals("RECOVERY_FAILED", recovered?.status)
        assertEquals(RecordingRecoveryState.MISSING_FILE, recovered?.recoveryState)
    }

    @Test
    fun interruptedRecordingWithZeroByteFile_isMarkedZeroByteAndFileIsKept() = runBlocking {
        val file = recordingsDir().resolve("zero.m4a")
        file.writeBytes(byteArrayOf())
        database.recordingDao().insertRecording(recording(id = "zero", status = "FINALIZING", fileName = file.name))

        scanner.scan()

        val recovered = database.recordingDao().getRecordingByIdSync("zero")
        assertEquals("RECOVERY_FAILED", recovered?.status)
        assertEquals(RecordingRecoveryState.ZERO_BYTE_FILE, recovered?.recoveryState)
        assertTrue(file.exists())
    }

    @Test
    fun playableInterruptedRecording_isMarkedSavedRecoveredAfterValidation() = runBlocking {
        val file = recordingsDir().resolve("playable.m4a")
        file.writeBytes(byteArrayOf(1, 2, 3))
        validator.results[file.absolutePath] = RecordingFileValidation(
            isPlayable = true,
            durationMs = 42_000L,
            mimeType = "audio/mp4a-latm",
            sampleRate = 48_000,
            channels = 1
        )
        database.recordingDao().insertRecording(recording(id = "playable", status = "RECORDING", fileName = file.name))

        scanner.scan()

        val recovered = database.recordingDao().getRecordingByIdSync("playable")
        assertEquals("SAVED", recovered?.status)
        assertEquals(RecordingRecoveryState.RECOVERED, recovered?.recoveryState)
        assertEquals(42_000L, recovered?.durationMs)
        assertEquals(file.length(), recovered?.sizeBytes)
        assertTrue(recovered?.title?.startsWith("Recovered") == true)
    }

    @Test
    fun corruptInterruptedRecording_isMarkedCorruptAndNotDeleted() = runBlocking {
        val file = recordingsDir().resolve("corrupt.m4a")
        file.writeBytes(byteArrayOf(9, 9, 9))
        validator.results[file.absolutePath] = RecordingFileValidation(
            isPlayable = false,
            durationMs = 0L,
            mimeType = null,
            sampleRate = null,
            channels = null
        )
        database.recordingDao().insertRecording(recording(id = "corrupt", status = "PAUSED", fileName = file.name))

        scanner.scan()

        val recovered = database.recordingDao().getRecordingByIdSync("corrupt")
        assertEquals("RECOVERY_FAILED", recovered?.status)
        assertEquals(RecordingRecoveryState.CORRUPT_FILE, recovered?.recoveryState)
        assertTrue(file.exists())
    }

    @Test
    fun orphanFile_createsDiagnosticRowAndRunningTwiceDoesNotDuplicate() = runBlocking {
        val file = recordingsDir().resolve("orphan.m4a")
        file.writeBytes(byteArrayOf(7, 7, 7))
        validator.results[file.absolutePath] = RecordingFileValidation(
            isPlayable = true,
            durationMs = 10_000L,
            mimeType = "audio/mp4a-latm",
            sampleRate = 44_100,
            channels = 2
        )

        scanner.scan()
        scanner.scan()

        val row = database.recordingDao().getRecordingByOriginalFileUriSync(file.toFileUri())
        assertEquals("RECOVERY_FAILED", row?.status)
        assertEquals(RecordingRecoveryState.ORPHAN_FILE, row?.recoveryState)
        assertEquals(10_000L, row?.durationMs)
        assertTrue(file.exists())
    }

    @Test
    fun savedRecordingWithFile_isNotReclassifiedByRecovery() = runBlocking {
        val file = recordingsDir().resolve("saved.m4a")
        file.writeBytes(byteArrayOf(5, 5, 5))
        database.recordingDao().insertRecording(recording(id = "saved", status = "SAVED", fileName = file.name))

        scanner.scan()

        val row = database.recordingDao().getRecordingByIdSync("saved")
        assertEquals("SAVED", row?.status)
        assertEquals("NORMAL", row?.recoveryState)
        assertTrue(file.exists())
    }

    @Test
    fun expiredTempFile_isDeletedByRetentionPolicy() = runBlocking {
        val file = recordingsDir().resolve("expired.tmp")
        file.writeBytes(byteArrayOf(1))
        file.setLastModified(
            System.currentTimeMillis() -
                RecordingRecoveryRetentionPolicy.TEMP_FILE_RETENTION_MS -
                1_000L
        )

        scanner.scan()

        assertFalse(file.exists())
    }

    @Test
    fun freshTempFile_isKeptByRetentionPolicy() = runBlocking {
        val file = recordingsDir().resolve("fresh.tmp")
        file.writeBytes(byteArrayOf(1))

        scanner.scan()

        assertTrue(file.exists())
    }

    private fun recordingsDir() = context.filesDir.resolve("recordings")

    private fun recording(id: String, status: String, fileName: String): RecordingEntity {
        val now = System.currentTimeMillis()
        return RecordingEntity(
            id = id,
            title = "Session $id",
            createdAt = now,
            updatedAt = now,
            durationMs = 0L,
            status = status,
            originalFileUri = recordingsDir().resolve(fileName).toFileUri(),
            editedOutputUri = null,
            mimeType = "audio/aac",
            sampleRate = 44_100,
            channels = 2,
            bitrate = 128_000,
            sizeBytes = 0L
        )
    }

    private fun java.io.File.toFileUri(): String = "file://$absolutePath"
}

private class FakeRecordingFileValidator : RecordingFileValidator {
    val results = mutableMapOf<String, RecordingFileValidation>()

    override fun validate(file: java.io.File): RecordingFileValidation {
        return results[file.absolutePath]
            ?: RecordingFileValidation(
                isPlayable = false,
                durationMs = 0L,
                mimeType = null,
                sampleRate = null,
                channels = null
            )
    }
}
