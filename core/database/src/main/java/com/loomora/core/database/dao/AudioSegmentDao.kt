package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loomora.core.database.entity.AudioSegmentEntity
import com.loomora.core.database.entity.BackgroundJobEntity
import com.loomora.core.database.entity.MarkerEntity
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
