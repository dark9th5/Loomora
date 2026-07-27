package com.loomora.core.audio.editor

import android.content.Context
import com.loomora.core.model.EditRecipe
import com.loomora.core.model.EditRecipeIssue
import com.loomora.core.model.EditRecipeValidation
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

@Singleton
class AudioEditExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val audioEditEngine: AudioEditEngine,
    private val metadataReader: AudioOutputMetadataReader
) {

    suspend fun exportEditedRecording(
        originalRecording: Recording,
        recipe: EditRecipe,
        onProgress: (Int) -> Unit = {}
    ): Result<Recording> = withContext(Dispatchers.IO) {
        val sourceFile = File(originalRecording.originalFileUri.removePrefix("file://"))
        if (!sourceFile.exists() || !sourceFile.isFile) {
            return@withContext Result.failure(AudioEditException.SourceMissing)
        }
        if (recipe.isSpeechClarityEnabled) {
            return@withContext Result.failure(AudioEditException.UnsupportedOperation)
        }

        val sourceFingerprint = AudioEditFingerprint.compute(sourceFile)
        val resolvedRecipe = recipe.copy(sourceFingerprint = sourceFingerprint)
        val validation = resolvedRecipe.validate(originalRecording.durationMs)
        val validRecipe = when (validation) {
            is EditRecipeValidation.Valid -> validation
            is EditRecipeValidation.Invalid -> {
                val failure = when (validation.issue) {
                    EditRecipeIssue.EMPTY_RESULT -> AudioEditException.EmptyResult
                    EditRecipeIssue.INVALID_RANGE -> AudioEditException.InvalidRecipe
                    EditRecipeIssue.UNSUPPORTED_OPERATION -> AudioEditException.UnsupportedOperation
                }
                return@withContext Result.failure(failure)
            }
        }

        val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val newId = UUID.randomUUID().toString()
        val tempOutputFile = File(recordingsDir, "${newId}_edited.tmp.m4a")
        val publishedOutputFile = File(recordingsDir, "${newId}_edited.m4a")

        try {
            coroutineContext.ensureActive()
            audioEditEngine.export(
                sourceFile = sourceFile,
                keepRanges = validRecipe.keepRanges,
                outputFile = tempOutputFile,
                onProgress = onProgress
            )
            coroutineContext.ensureActive()

            val outputMetadata = metadataReader.read(tempOutputFile)
                ?: return@withContext Result.failure(AudioEditException.OutputValidationFailed)

            if (!isExpectedDuration(outputMetadata.durationMs, validRecipe.outputDurationMs)) {
                tempOutputFile.delete()
                return@withContext Result.failure(AudioEditException.OutputValidationFailed)
            }

            publishTempFile(tempOutputFile, publishedOutputFile)

            val timestamp = System.currentTimeMillis()
            val editedRecording = Recording(
                id = newId,
                title = "${originalRecording.title} (Edited)",
                createdAt = timestamp,
                updatedAt = timestamp,
                durationMs = outputMetadata.durationMs,
                status = RecordingStatus.SAVED,
                originalFileUri = "file://${publishedOutputFile.absolutePath}",
                editedOutputUri = "file://${publishedOutputFile.absolutePath}",
                mimeType = outputMetadata.mimeType,
                sampleRate = outputMetadata.sampleRate ?: originalRecording.sampleRate,
                channels = outputMetadata.channelCount ?: originalRecording.channels,
                bitrate = outputMetadata.bitrate ?: originalRecording.bitrate,
                sizeBytes = outputMetadata.sizeBytes,
                languageHint = originalRecording.languageHint,
                isFavorite = originalRecording.isFavorite
            )

            recordingRepository.insertRecording(editedRecording)
            Result.success(editedRecording)
        } catch (_: CancellationException) {
            tempOutputFile.delete()
            publishedOutputFile.delete()
            Result.failure(AudioEditException.ExportCancelled)
        } catch (exception: Exception) {
            tempOutputFile.delete()
            publishedOutputFile.delete()
            Result.failure(exception)
        }
    }

    private fun isExpectedDuration(actualDurationMs: Long, expectedDurationMs: Long): Boolean {
        val toleranceMs = maxOf(250L, expectedDurationMs / 20L)
        return abs(actualDurationMs - expectedDurationMs) <= toleranceMs
    }

    private fun publishTempFile(tempFile: File, publishedFile: File) {
        if (publishedFile.exists()) {
            publishedFile.delete()
        }
        if (!tempFile.renameTo(publishedFile)) {
            tempFile.copyTo(publishedFile, overwrite = false)
            tempFile.delete()
        }
    }
}
