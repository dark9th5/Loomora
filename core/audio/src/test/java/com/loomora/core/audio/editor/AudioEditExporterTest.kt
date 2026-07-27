package com.loomora.core.audio.editor

import androidx.test.core.app.ApplicationProvider
import com.loomora.core.model.EditOperation
import com.loomora.core.model.EditRecipe
import com.loomora.core.model.KeepRange
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AudioEditExporterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun exportEditedRecording_persistsValidatedOutput_andKeepsOriginalHash() = runTest {
        val originalFile = temporaryFolder.newFile("original.m4a").apply {
            writeBytes(ByteArray(128) { index -> index.toByte() })
        }
        val originalHash = AudioEditFingerprint.compute(originalFile)
        val repository = FakeRecordingRepository()
        val metadata = ExportedAudioMetadata(
            durationMs = 25_000L,
            mimeType = "audio/mp4a-latm",
            sampleRate = 44_100,
            channelCount = 2,
            bitrate = 128_000,
            sizeBytes = 96L
        )
        val exporter = AudioEditExporter(
            context = ApplicationProvider.getApplicationContext(),
            recordingRepository = repository,
            audioEditEngine = FakeAudioEditEngine { _, _, outputFile, onProgress ->
                outputFile.writeBytes(ByteArray(96) { 7 })
                onProgress(100)
            },
            metadataReader = FakeAudioOutputMetadataReader(metadata)
        )

        val result = exporter.exportEditedRecording(
            originalRecording = originalRecording(originalFile),
            recipe = EditRecipe(
                originalRecordingId = "rec-1",
                operations = listOf(EditOperation.Trim(5_000L, 30_000L))
            )
        )

        assertTrue(result.isSuccess)
        val exportedRecording = result.getOrNull()
        assertNotNull(exportedRecording)
        assertEquals(25_000L, exportedRecording?.durationMs)
        assertEquals("audio/mp4a-latm", exportedRecording?.mimeType)
        assertEquals(44_100, exportedRecording?.sampleRate)
        assertEquals(2, exportedRecording?.channels)
        assertEquals(128_000, exportedRecording?.bitrate)
        assertEquals(96L, exportedRecording?.sizeBytes)
        assertEquals(originalHash, AudioEditFingerprint.compute(originalFile))
        assertEquals(1, repository.inserted.size)
    }

    @Test
    fun exportEditedRecording_withInvalidRecipe_rejectsBeforeEngineRuns() = runTest {
        var engineCalled = false
        val originalFile = temporaryFolder.newFile("invalid-source.m4a").apply {
            writeBytes(ByteArray(64) { 1 })
        }
        val exporter = AudioEditExporter(
            context = ApplicationProvider.getApplicationContext(),
            recordingRepository = FakeRecordingRepository(),
            audioEditEngine = FakeAudioEditEngine { _, _, _, _ -> engineCalled = true },
            metadataReader = FakeAudioOutputMetadataReader(null)
        )

        val result = exporter.exportEditedRecording(
            originalRecording = originalRecording(originalFile),
            recipe = EditRecipe(
                originalRecordingId = "rec-1",
                operations = listOf(EditOperation.Trim(25_000L, 10_000L))
            )
        )

        assertTrue(result.exceptionOrNull() is AudioEditException.InvalidRecipe)
        assertFalse(engineCalled)
    }

    @Test
    fun exportEditedRecording_withEmptyResult_rejectsRecipe() = runTest {
        val originalFile = temporaryFolder.newFile("empty-source.m4a").apply {
            writeBytes(ByteArray(64) { 1 })
        }
        val exporter = AudioEditExporter(
            context = ApplicationProvider.getApplicationContext(),
            recordingRepository = FakeRecordingRepository(),
            audioEditEngine = FakeAudioEditEngine { _, _, _, _ -> error("Should not run engine") },
            metadataReader = FakeAudioOutputMetadataReader(null)
        )

        val result = exporter.exportEditedRecording(
            originalRecording = originalRecording(originalFile),
            recipe = EditRecipe(
                originalRecordingId = "rec-1",
                operations = listOf(EditOperation.DeleteRange(0L, 60_000L))
            )
        )

        assertTrue(result.exceptionOrNull() is AudioEditException.EmptyResult)
    }

    @Test
    fun exportEditedRecording_cancelCleansTempFile() = runTest {
        val originalFile = temporaryFolder.newFile("cancel-source.m4a").apply {
            writeBytes(ByteArray(64) { 1 })
        }
        val repository = FakeRecordingRepository()
        val exporter = AudioEditExporter(
            context = ApplicationProvider.getApplicationContext(),
            recordingRepository = repository,
            audioEditEngine = FakeAudioEditEngine { _, _, outputFile, _ ->
                outputFile.writeBytes(ByteArray(32) { 2 })
                throw CancellationException("cancel")
            },
            metadataReader = FakeAudioOutputMetadataReader(null)
        )

        val result = exporter.exportEditedRecording(
            originalRecording = originalRecording(originalFile),
            recipe = EditRecipe(
                originalRecordingId = "rec-1",
                operations = listOf(EditOperation.Trim(0L, 10_000L))
            )
        )

        assertTrue(result.exceptionOrNull() is AudioEditException.ExportCancelled)
        assertEquals(0, repository.inserted.size)
        val recordingsDir = File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "recordings")
        assertTrue(recordingsDir.listFiles().orEmpty().none { it.name.endsWith(".tmp.m4a") })
    }

    @Test
    fun exportEditedRecording_withCorruptSource_returnsSourceMissing() = runTest {
        val exporter = AudioEditExporter(
            context = ApplicationProvider.getApplicationContext(),
            recordingRepository = FakeRecordingRepository(),
            audioEditEngine = FakeAudioEditEngine { _, _, _, _ -> },
            metadataReader = FakeAudioOutputMetadataReader(null)
        )

        val result = exporter.exportEditedRecording(
            originalRecording = originalRecording(File(temporaryFolder.root, "missing.m4a")),
            recipe = EditRecipe(originalRecordingId = "rec-1")
        )

        assertTrue(result.exceptionOrNull() is AudioEditException.SourceMissing)
    }

    @Test
    fun exportEditedRecording_withBadMetadataFailsValidation() = runTest {
        val originalFile = temporaryFolder.newFile("bad-metadata-source.m4a").apply {
            writeBytes(ByteArray(64) { 1 })
        }
        val exporter = AudioEditExporter(
            context = ApplicationProvider.getApplicationContext(),
            recordingRepository = FakeRecordingRepository(),
            audioEditEngine = FakeAudioEditEngine { _, _, outputFile, _ ->
                outputFile.writeBytes(ByteArray(48) { 9 })
            },
            metadataReader = FakeAudioOutputMetadataReader(
                ExportedAudioMetadata(
                    durationMs = 1_000L,
                    mimeType = "audio/mp4a-latm",
                    sampleRate = 44_100,
                    channelCount = 2,
                    bitrate = 128_000,
                    sizeBytes = 48L
                )
            )
        )

        val result = exporter.exportEditedRecording(
            originalRecording = originalRecording(originalFile),
            recipe = EditRecipe(
                originalRecordingId = "rec-1",
                operations = listOf(EditOperation.Trim(5_000L, 30_000L))
            )
        )

        assertTrue(result.exceptionOrNull() is AudioEditException.OutputValidationFailed)
    }

    private fun originalRecording(sourceFile: File): Recording {
        return Recording(
            id = "rec-1",
            title = "Meeting",
            createdAt = 1L,
            updatedAt = 1L,
            durationMs = 60_000L,
            status = RecordingStatus.SAVED,
            originalFileUri = "file://${sourceFile.absolutePath}"
        )
    }
}

private class FakeRecordingRepository : RecordingRepository {
    val inserted = mutableListOf<Recording>()

    override fun getActiveRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getRecoveryDiagnostics(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getFavoriteRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getTrashedRecordings(): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override fun getRecordingById(id: String): Flow<Recording?> = MutableStateFlow(null)
    override fun searchRecordings(query: String): Flow<List<Recording>> = MutableStateFlow(emptyList())
    override suspend fun insertRecording(recording: Recording) {
        inserted += recording
    }

    override suspend fun renameRecording(id: String, newTitle: String): RecordingOperationResult = RecordingOperationResult.Success
    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) = Unit
    override suspend fun softDeleteRecording(id: String): RecordingOperationResult = RecordingOperationResult.Success
    override suspend fun restoreRecording(id: String): RecordingOperationResult = RecordingOperationResult.Success
    override suspend fun deleteRecordingPermanently(id: String): RecordingOperationResult = RecordingOperationResult.Success
}

private class FakeAudioEditEngine(
    private val block: suspend (File, List<KeepRange>, File, (Int) -> Unit) -> Unit
) : AudioEditEngine {
    override suspend fun export(
        sourceFile: File,
        keepRanges: List<KeepRange>,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) {
        block(sourceFile, keepRanges, outputFile, onProgress)
    }
}

private class FakeAudioOutputMetadataReader(
    private val metadata: ExportedAudioMetadata?
) : AudioOutputMetadataReader {
    override fun read(file: File): ExportedAudioMetadata? = metadata
}
