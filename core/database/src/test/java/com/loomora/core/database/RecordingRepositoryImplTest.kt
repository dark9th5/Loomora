package com.loomora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.database.repository.RecordingFileSystem
import com.loomora.core.database.repository.RecordingRepositoryImpl
import com.loomora.core.model.RecordingOperationResult
import kotlinx.coroutines.flow.first
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
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecordingRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: LoomoraDatabase
    private lateinit var repository: RecordingRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            LoomoraDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RecordingRepositoryImpl(
            recordingDao = database.recordingDao(),
            context = context,
            fileSystem = object : RecordingFileSystem {
                override fun stageForDeletion(source: File, stagingDir: File): File {
                    stagingDir.mkdirs()
                    val staged = File(stagingDir, source.name)
                    Files.move(
                        source.toPath(),
                        staged.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                    return staged
                }

                override fun restoreFromStaging(stagedFile: File, destination: File) {
                    destination.parentFile?.mkdirs()
                    Files.move(
                        stagedFile.toPath(),
                        destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                }

                override fun deleteIfExists(file: File): Boolean {
                    return !file.exists() || file.delete()
                }
            }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun renameSoftDeleteAndRestore_persistStateChanges() = runBlocking {
        database.recordingDao().insertRecording(recording("rec-1"))

        assertEquals(RecordingOperationResult.Success, repository.renameRecording("rec-1", "Renamed"))
        assertEquals("Renamed", database.recordingDao().getRecordingById("rec-1").first()?.title)

        assertEquals(RecordingOperationResult.Success, repository.softDeleteRecording("rec-1"))
        assertEquals(1, database.recordingDao().getTrashedRecordings().first().size)

        assertEquals(RecordingOperationResult.Success, repository.restoreRecording("rec-1"))
        assertEquals(1, database.recordingDao().getActiveRecordings().first().size)
    }

    @Test
    fun permanentDelete_removesRowAndLocalFile() = runBlocking {
        val sourceFile = tempFolder.newFile("rec-2.m4a").apply {
            writeText("audio")
        }
        database.recordingDao().insertRecording(
            recording(
                id = "rec-2",
                originalFileUri = sourceFile.toUriString()
            )
        )

        val result = repository.deleteRecordingPermanently("rec-2")

        assertEquals(RecordingOperationResult.Success, result)
        assertEquals(null, database.recordingDao().getRecordingById("rec-2").first())
        assertFalse(sourceFile.exists())
    }

    @Test
    fun permanentDelete_missingSourceReturnsTypedResultWithoutLeavingRow() = runBlocking {
        val missingFile = File(tempFolder.root, "missing.m4a")
        database.recordingDao().insertRecording(
            recording(
                id = "rec-3",
                originalFileUri = missingFile.toUriString()
            )
        )

        val result = repository.deleteRecordingPermanently("rec-3")

        assertEquals(RecordingOperationResult.SourceMissing, result)
        assertEquals(null, database.recordingDao().getRecordingById("rec-3").first())
    }

    @Test
    fun permanentDelete_failedFilesystemDeleteReturnsFailureAfterRowRemoval() = runBlocking {
        val sourceFile = tempFolder.newFile("rec-4.m4a").apply {
            writeText("audio")
        }
        database.recordingDao().insertRecording(
            recording(
                id = "rec-4",
                originalFileUri = sourceFile.toUriString()
            )
        )

        val repositoryWithDeleteFailure = RecordingRepositoryImpl(
            recordingDao = database.recordingDao(),
            context = ApplicationProvider.getApplicationContext(),
            fileSystem = object : RecordingFileSystem {
                override fun stageForDeletion(source: File, stagingDir: File): File {
                    stagingDir.mkdirs()
                    val staged = File(stagingDir, source.name)
                    Files.move(source.toPath(), staged.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    return staged
                }

                override fun restoreFromStaging(stagedFile: File, destination: File) {
                    destination.parentFile?.mkdirs()
                    Files.move(stagedFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                override fun deleteIfExists(file: File): Boolean = false
            }
        )

        val result = repositoryWithDeleteFailure.deleteRecordingPermanently("rec-4")

        assertTrue(result is RecordingOperationResult.FileSystemFailure)
        assertEquals(null, database.recordingDao().getRecordingById("rec-4").first())
        val stagedFile = File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "pending_delete/${sourceFile.name}")
        assertTrue(stagedFile.exists())
    }

    private fun File.toUriString(): String = "file://$absolutePath"

    private fun recording(
        id: String,
        originalFileUri: String = "file:///tmp/$id.m4a"
    ) = RecordingEntity(
        id = id,
        title = "Recording $id",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        durationMs = 30_000L,
        status = "SAVED",
        originalFileUri = originalFileUri,
        editedOutputUri = null,
        mimeType = "audio/mp4",
        sampleRate = 44_100,
        channels = 1,
        bitrate = 128_000,
        sizeBytes = 256L,
        languageHint = "en",
        isFavorite = false,
        deletedAt = null
    )
}
