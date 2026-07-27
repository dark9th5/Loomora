package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.loomora.core.database.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingById(id: String): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingByIdSync(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL AND status = 'SAVED' ORDER BY createdAt DESC")
    fun getActiveRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL AND status = 'RECOVERY_FAILED' ORDER BY createdAt DESC")
    fun getRecoveryDiagnostics(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE status IN ('RECORDING', 'PAUSED', 'FINALIZING') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentRecordingSession(): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE status IN ('RECORDING', 'PAUSED', 'FINALIZING')")
    suspend fun getInterruptedRecordingSessions(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE originalFileUri = :fileUri LIMIT 1")
    suspend fun getRecordingByOriginalFileUriSync(fileUri: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL AND status = 'SAVED' AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL AND status = 'SAVED' AND (title LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE recordings SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameRecording(id: String, title: String, updatedAt: Long): Int

    @Query("UPDATE recordings SET status = :status, durationMs = :durationMs, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRecordingStatus(id: String, status: String, durationMs: Long, updatedAt: Long)

    @Query("UPDATE recordings SET transcriptStatus = :transcriptStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTranscriptStatus(id: String, transcriptStatus: String, updatedAt: Long)

    @Query("UPDATE recordings SET title = :title, status = :status, recoveryState = :recoveryState, durationMs = :durationMs, sizeBytes = :sizeBytes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRecoveredRecording(
        id: String,
        title: String,
        status: String,
        recoveryState: String,
        durationMs: Long,
        sizeBytes: Long,
        updatedAt: Long
    )

    @Query("UPDATE recordings SET status = :status, recoveryState = :recoveryState, sizeBytes = :sizeBytes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRecoveryFailure(
        id: String,
        status: String,
        recoveryState: String,
        sizeBytes: Long,
        updatedAt: Long
    )

    @Query("UPDATE recordings SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteRecording(id: String, deletedAt: Long, updatedAt: Long): Int

    @Query("UPDATE recordings SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreRecording(id: String, updatedAt: Long): Int

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingPermanently(id: String): Int
}
