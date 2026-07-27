package com.loomora.core.audio.recovery

import android.content.Context
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.model.RecordingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRecoveryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: RecordingDao,
    private val validator: RecordingFileValidator
) {
    suspend fun scan() {
        recoverInterruptedRows()
        recordOrphanFiles()
        cleanupExpiredTempFiles()
    }

    private suspend fun recoverInterruptedRows() {
        recordingDao.getInterruptedRecordingSessions().forEach { recording ->
            val file = recording.originalFileUri.toFileOrNull()
            when {
                file == null || !file.exists() || !file.isFile -> {
                    recordingDao.updateRecoveryFailure(
                        id = recording.id,
                        status = RecordingStatus.RECOVERY_FAILED.name,
                        recoveryState = RecordingRecoveryState.MISSING_FILE,
                        sizeBytes = 0L,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                file.length() == 0L -> {
                    recordingDao.updateRecoveryFailure(
                        id = recording.id,
                        status = RecordingStatus.RECOVERY_FAILED.name,
                        recoveryState = RecordingRecoveryState.ZERO_BYTE_FILE,
                        sizeBytes = 0L,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                else -> recoverNonEmptyInterruptedRow(recording, file)
            }
        }
    }

    private suspend fun recoverNonEmptyInterruptedRow(recording: RecordingEntity, file: File) {
        val validation = validator.validate(file)
        if (validation.isPlayable) {
            recordingDao.updateRecoveredRecording(
                id = recording.id,
                title = recoveredTitle(recording.title),
                status = RecordingStatus.SAVED.name,
                recoveryState = RecordingRecoveryState.RECOVERED,
                durationMs = validation.durationMs,
                sizeBytes = file.length(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            recordingDao.updateRecoveryFailure(
                id = recording.id,
                status = RecordingStatus.RECOVERY_FAILED.name,
                recoveryState = RecordingRecoveryState.CORRUPT_FILE,
                sizeBytes = file.length(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private suspend fun recordOrphanFiles() {
        recordingsDir().listFiles()
            ?.filter { it.isFile && it.extension.equals("m4a", ignoreCase = true) }
            ?.forEach { file ->
                val fileUri = file.toFileUri()
                if (recordingDao.getRecordingByOriginalFileUriSync(fileUri) != null) {
                    return@forEach
                }

                val validation = if (file.length() > 0L) {
                    validator.validate(file)
                } else {
                    RecordingFileValidation(
                        isPlayable = false,
                        durationMs = 0L,
                        mimeType = null,
                        sampleRate = null,
                        channels = null
                    )
                }
                val now = System.currentTimeMillis()
                recordingDao.insertRecording(
                    RecordingEntity(
                        id = UUID.nameUUIDFromBytes(fileUri.toByteArray()).toString(),
                        title = orphanTitle(file.nameWithoutExtension),
                        createdAt = file.lastModified().takeIf { it > 0L } ?: now,
                        updatedAt = now,
                        durationMs = if (validation.isPlayable) validation.durationMs else 0L,
                        status = RecordingStatus.RECOVERY_FAILED.name,
                        originalFileUri = fileUri,
                        editedOutputUri = null,
                        mimeType = validation.mimeType ?: "audio/aac",
                        sampleRate = validation.sampleRate ?: 44100,
                        channels = validation.channels ?: 2,
                        bitrate = 128000,
                        sizeBytes = file.length(),
                        recoveryState = RecordingRecoveryState.ORPHAN_FILE
                    )
                )
            }
    }

    private fun recordingsDir(): File = File(context.filesDir, "recordings")

    private fun cleanupExpiredTempFiles() {
        val cutoff = System.currentTimeMillis() - RecordingRecoveryRetentionPolicy.TEMP_FILE_RETENTION_MS
        recordingsDir().listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.extension.equals(
                        RecordingRecoveryRetentionPolicy.TEMP_FILE_EXTENSION,
                        ignoreCase = true
                    ) &&
                    file.lastModified() in 1 until cutoff
            }
            ?.forEach { it.delete() }
    }

    private fun String.toFileOrNull(): File? {
        val path = removePrefix("file://")
        if (path.isBlank() || path.contains("..")) {
            return null
        }
        return File(path)
    }

    private fun File.toFileUri(): String = "file://$absolutePath"

    private fun recoveredTitle(title: String): String {
        return if (title.startsWith(RECOVERED_PREFIX)) title else "$RECOVERED_PREFIX $title"
    }

    private fun orphanTitle(stem: String): String = "$RECOVERED_PREFIX $stem"

    private companion object {
        const val RECOVERED_PREFIX = "Recovered"
    }
}
