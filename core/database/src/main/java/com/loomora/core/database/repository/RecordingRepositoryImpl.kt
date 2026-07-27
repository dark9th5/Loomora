package com.loomora.core.database.repository

import android.content.Context
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    @ApplicationContext private val context: Context,
    private val fileSystem: RecordingFileSystem
) : RecordingRepository {

    override fun getActiveRecordings(): Flow<List<Recording>> {
        return recordingDao.getActiveRecordings().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecoveryDiagnostics(): Flow<List<Recording>> {
        return recordingDao.getRecoveryDiagnostics().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFavoriteRecordings(): Flow<List<Recording>> {
        return recordingDao.getFavoriteRecordings().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTrashedRecordings(): Flow<List<Recording>> {
        return recordingDao.getTrashedRecordings().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecordingById(id: String): Flow<Recording?> {
        return recordingDao.getRecordingById(id).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun searchRecordings(query: String): Flow<List<Recording>> {
        return recordingDao.searchRecordings(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertRecording(recording: Recording) {
        recordingDao.insertRecording(recording.toEntity())
    }

    override suspend fun renameRecording(id: String, newTitle: String): RecordingOperationResult {
        if (newTitle.isBlank()) {
            return RecordingOperationResult.DatabaseFailure("Recording title cannot be blank")
        }
        return try {
            if (recordingDao.renameRecording(id, newTitle.trim(), System.currentTimeMillis()) > 0) {
                RecordingOperationResult.Success
            } else {
                RecordingOperationResult.NotFound
            }
        } catch (exception: Exception) {
            RecordingOperationResult.DatabaseFailure(exception.message ?: "Rename failed")
        }
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        recordingDao.setFavorite(id, isFavorite, System.currentTimeMillis())
    }

    override suspend fun softDeleteRecording(id: String): RecordingOperationResult {
        return try {
            if (recordingDao.softDeleteRecording(id, System.currentTimeMillis(), System.currentTimeMillis()) > 0) {
                RecordingOperationResult.Success
            } else {
                RecordingOperationResult.NotFound
            }
        } catch (exception: Exception) {
            RecordingOperationResult.DatabaseFailure(exception.message ?: "Soft delete failed")
        }
    }

    override suspend fun restoreRecording(id: String): RecordingOperationResult {
        return try {
            if (recordingDao.restoreRecording(id, System.currentTimeMillis()) > 0) {
                RecordingOperationResult.Success
            } else {
                RecordingOperationResult.NotFound
            }
        } catch (exception: Exception) {
            RecordingOperationResult.DatabaseFailure(exception.message ?: "Restore failed")
        }
    }

    override suspend fun deleteRecordingPermanently(id: String): RecordingOperationResult {
        val recording = recordingDao.getRecordingByIdSync(id) ?: return RecordingOperationResult.NotFound
        val stagingDir = File(context.filesDir, "pending_delete")
        val originals = localFilesForRecording(recording)
        val stagedFiles = mutableListOf<Pair<File, File>>()

        try {
            originals.forEach { source ->
                if (!source.exists()) {
                    return@forEach
                }
                val staged = fileSystem.stageForDeletion(source, stagingDir)
                stagedFiles += staged to source
            }
        } catch (exception: Exception) {
            stagedFiles.forEach { (staged, original) ->
                runCatching { fileSystem.restoreFromStaging(staged, original) }
            }
            return RecordingOperationResult.FileSystemFailure(
                exception.message ?: "Unable to stage files for deletion"
            )
        }

        val deleteCount = try {
            recordingDao.deleteRecordingPermanently(id)
        } catch (exception: Exception) {
            stagedFiles.forEach { (staged, original) ->
                runCatching { fileSystem.restoreFromStaging(staged, original) }
            }
            return RecordingOperationResult.DatabaseFailure(
                exception.message ?: "Unable to remove recording row"
            )
        }

        if (deleteCount <= 0) {
            stagedFiles.forEach { (staged, original) ->
                runCatching { fileSystem.restoreFromStaging(staged, original) }
            }
            return RecordingOperationResult.NotFound
        }

        stagedFiles.forEach { (staged, _) ->
            if (!fileSystem.deleteIfExists(staged)) {
                return RecordingOperationResult.FileSystemFailure(
                    "Recording row was removed, but a staged file could not be deleted"
                )
            }
        }

        return if (originals.isNotEmpty() && originals.none { it.exists() } && stagedFiles.isEmpty()) {
            RecordingOperationResult.SourceMissing
        } else {
            RecordingOperationResult.Success
        }
    }

    private fun localFilesForRecording(recording: RecordingEntity): List<File> {
        val files = buildList {
            recording.originalFileUri.toLocalFileOrNull()?.let(::add)
            recording.editedOutputUri?.toLocalFileOrNull()?.let(::add)
        }
        return files.distinctBy { it.absolutePath }
    }

    private fun String.toLocalFileOrNull(): File? {
        val path = removePrefix("file://")
        if (path.isBlank() || path.contains("..")) {
            return null
        }
        return File(path)
    }

    private fun RecordingEntity.toDomainModel(): Recording {
        return Recording(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            durationMs = durationMs,
            status = runCatching { RecordingStatus.valueOf(status) }.getOrDefault(RecordingStatus.SAVED),
            originalFileUri = originalFileUri,
            editedOutputUri = editedOutputUri,
            mimeType = mimeType,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate,
            sizeBytes = sizeBytes,
            languageHint = languageHint,
            isFavorite = isFavorite,
            deletedAt = deletedAt,
            recoveryState = recoveryState,
            transcriptStatus = transcriptStatus,
            insightStatus = insightStatus
        )
    }

    private fun Recording.toEntity(): RecordingEntity {
        return RecordingEntity(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            durationMs = durationMs,
            status = status.name,
            originalFileUri = originalFileUri,
            editedOutputUri = editedOutputUri,
            mimeType = mimeType,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate,
            sizeBytes = sizeBytes,
            languageHint = languageHint,
            isFavorite = isFavorite,
            deletedAt = deletedAt,
            recoveryState = recoveryState,
            transcriptStatus = transcriptStatus,
            insightStatus = insightStatus
        )
    }
}
