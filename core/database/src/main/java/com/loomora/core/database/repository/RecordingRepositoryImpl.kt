package com.loomora.core.database.repository

import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao
) : RecordingRepository {

    override fun getActiveRecordings(): Flow<List<Recording>> {
        return recordingDao.getActiveRecordings().map { entities ->
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

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        recordingDao.setFavorite(id, isFavorite, System.currentTimeMillis())
    }

    override suspend fun softDeleteRecording(id: String) {
        recordingDao.softDeleteRecording(id, System.currentTimeMillis(), System.currentTimeMillis())
    }

    override suspend fun restoreRecording(id: String) {
        recordingDao.restoreRecording(id, System.currentTimeMillis())
    }

    override suspend fun deleteRecordingPermanently(id: String) {
        val recording = recordingDao.getRecordingByIdSync(id) ?: return

        // Safety check: ensure file path deletion does not traverse arbitrary system paths
        recording.originalFileUri.let { uri ->
            if (uri.startsWith("file://") || uri.startsWith("/")) {
                val filePath = uri.removePrefix("file://")
                val file = File(filePath)
                if (file.exists() && file.isFile && !filePath.contains("..")) {
                    file.delete()
                }
            }
        }

        recording.editedOutputUri?.let { uri ->
            if (uri.startsWith("file://") || uri.startsWith("/")) {
                val filePath = uri.removePrefix("file://")
                val file = File(filePath)
                if (file.exists() && file.isFile && !filePath.contains("..")) {
                    file.delete()
                }
            }
        }

        recordingDao.deleteRecordingPermanently(id)
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
