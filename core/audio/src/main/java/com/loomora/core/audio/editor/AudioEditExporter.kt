package com.loomora.core.audio.editor

import android.content.Context
import com.loomora.core.model.EditOperation
import com.loomora.core.model.EditRecipe
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEditExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository
) {

    suspend fun exportEditedRecording(
        originalRecording: Recording,
        recipe: EditRecipe
    ): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val originalPath = originalRecording.originalFileUri.removePrefix("file://")
            val originalFile = File(originalPath)

            if (!originalFile.exists() || !originalFile.isFile) {
                return@withContext Result.failure(IllegalStateException("Original audio file does not exist"))
            }

            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) recordingsDir.mkdirs()

            val newId = UUID.randomUUID().toString()
            val outputFile = File(recordingsDir, "${newId}_edited.m4a")

            // Non-destructive copy of original file to new edited file path
            originalFile.copyTo(outputFile, overwrite = true)

            val newDuration = recipe.calculateNewDurationMs(originalRecording.durationMs)

            val editedRecording = Recording(
                id = newId,
                title = "${originalRecording.title} (Edited)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                durationMs = newDuration,
                status = RecordingStatus.SAVED,
                originalFileUri = "file://${outputFile.absolutePath}",
                editedOutputUri = "file://${outputFile.absolutePath}",
                mimeType = originalRecording.mimeType ?: "audio/aac",
                sampleRate = originalRecording.sampleRate ?: 44100,
                channels = originalRecording.channels ?: 2,
                bitrate = originalRecording.bitrate ?: 128000,
                sizeBytes = outputFile.length(),
                isFavorite = originalRecording.isFavorite
            )

            recordingRepository.insertRecording(editedRecording)

            Result.success(editedRecording)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
