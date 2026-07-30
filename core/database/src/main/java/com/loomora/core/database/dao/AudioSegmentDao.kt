package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loomora.core.database.entity.AudioSegmentEntity
import com.loomora.core.database.entity.AnalysisJobEntity
import com.loomora.core.database.entity.BackgroundJobEntity
import com.loomora.core.database.entity.MarkerEntity
import com.loomora.core.database.entity.OfflineModelEntity
import com.loomora.core.database.entity.RecordingTagCrossRef
import com.loomora.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: AudioSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<AudioSegmentEntity>)

    @Query("SELECT * FROM audio_segments WHERE recordingId = :recordingId ORDER BY orderIndex ASC")
    fun getSegmentsForRecording(recordingId: String): Flow<List<AudioSegmentEntity>>

    @Query("SELECT * FROM audio_segments WHERE recordingId = :recordingId ORDER BY orderIndex ASC")
    suspend fun getSegmentsForRecordingSync(recordingId: String): List<AudioSegmentEntity>
}

@Dao
interface MarkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: MarkerEntity)

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY timeMs ASC")
    fun getMarkersForRecording(recordingId: String): Flow<List<MarkerEntity>>

    @Query("SELECT COUNT(*) FROM markers WHERE recordingId = :recordingId")
    fun getMarkerCountForRecording(recordingId: String): Flow<Int>

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteMarker(id: String)
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkTagToRecording(crossRef: RecordingTagCrossRef)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>
}

@Dao
interface BackgroundJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: BackgroundJobEntity)

    @Query("SELECT * FROM background_jobs WHERE state = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingJobs(): Flow<List<BackgroundJobEntity>>

    @Query("UPDATE background_jobs SET state = :state, progress = :progress, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateJobProgress(id: String, state: String, progress: Float, updatedAt: Long)
}

@Dao
interface OfflineModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModel(model: OfflineModelEntity)

    @Query("SELECT * FROM offline_models ORDER BY capability ASC, modelId ASC")
    fun observeModels(): Flow<List<OfflineModelEntity>>

    @Query("SELECT * FROM offline_models ORDER BY capability ASC, modelId ASC")
    suspend fun getAllModels(): List<OfflineModelEntity>

    @Query("SELECT * FROM offline_models WHERE modelId = :modelId")
    suspend fun getModelById(modelId: String): OfflineModelEntity?

    @Query("DELETE FROM offline_models WHERE modelId = :modelId")
    suspend fun deleteModel(modelId: String)
}

@Dao
interface AnalysisJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: AnalysisJobEntity)

    @Query("SELECT * FROM analysis_jobs WHERE status IN ('QUEUED','RUNNING','CANCEL_REQUESTED','RETRYABLE_FAILURE') ORDER BY createdAt ASC")
    fun observePendingJobs(): Flow<List<AnalysisJobEntity>>

    @Query("SELECT * FROM analysis_jobs WHERE recordingId = :recordingId ORDER BY createdAt DESC")
    fun observeJobsForRecording(recordingId: String): Flow<List<AnalysisJobEntity>>

    @Query("SELECT * FROM analysis_jobs WHERE logicalKey = :logicalKey LIMIT 1")
    suspend fun getJobByLogicalKey(logicalKey: String): AnalysisJobEntity?

    @Query("SELECT * FROM analysis_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: String): AnalysisJobEntity?

    @Query("UPDATE analysis_jobs SET status = :status, stage = :stage, progress = :progress, checkpointRef = :checkpointRef, stageOutputRef = :stageOutputRef, modelVersionsJson = :modelVersionsJson, errorCode = :errorCode, skipReason = :skipReason, fallbackReason = :fallbackReason, startedAt = :startedAt, finishedAt = :finishedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateJobState(
        id: String,
        status: String,
        stage: String,
        progress: Float,
        checkpointRef: String?,
        stageOutputRef: String?,
        modelVersionsJson: String,
        errorCode: String?,
        skipReason: String?,
        fallbackReason: String?,
        startedAt: Long?,
        finishedAt: Long?,
        updatedAt: Long
    )

    @Query("UPDATE analysis_jobs SET workRequestId = :workRequestId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateWorkRequestId(id: String, workRequestId: String, updatedAt: Long)

    @Query("UPDATE analysis_jobs SET timingsJson = :timingsJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTimings(id: String, timingsJson: String, updatedAt: Long)

    @Query("UPDATE analysis_jobs SET status = 'CANCEL_REQUESTED', updatedAt = :updatedAt WHERE id = :id AND status NOT IN ('COMPLETED','CANCELLED','TERMINAL_FAILURE','INVALIDATED')")
    suspend fun requestCancel(id: String, updatedAt: Long)

    @Query("UPDATE analysis_jobs SET status = 'QUEUED', stage = 'PREPARING_AUDIO', updatedAt = :updatedAt WHERE status = 'RUNNING'")
    suspend fun reconcileRunningToQueued(updatedAt: Long)
}
