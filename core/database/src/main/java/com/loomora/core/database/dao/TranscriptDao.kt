package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loomora.core.database.entity.TranscriptRevisionEntity
import com.loomora.core.database.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcript_revisions WHERE recordingId = :recordingId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestRevision(recordingId: String): Flow<TranscriptRevisionEntity?>

    @Query("SELECT * FROM transcript_segments WHERE revisionId = :revisionId ORDER BY orderIndex ASC")
    fun observeSegmentsForRevision(revisionId: String): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_revisions WHERE recordingId = :recordingId AND sourceFingerprint = :sourceFingerprint AND pipelineVersion = :pipelineVersion AND modelId = :modelId AND modelVersion = :modelVersion LIMIT 1")
    suspend fun getRevisionByIdentity(
        recordingId: String,
        sourceFingerprint: String,
        pipelineVersion: String,
        modelId: String,
        modelVersion: String
    ): TranscriptRevisionEntity?

    @Query("SELECT * FROM transcript_segments WHERE revisionId = :revisionId ORDER BY orderIndex ASC")
    suspend fun getSegmentsForRevisionSync(revisionId: String): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(revision: TranscriptRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    @Query("DELETE FROM transcript_segments WHERE revisionId = :revisionId")
    suspend fun deleteSegmentsForRevision(revisionId: String)

    @Transaction
    suspend fun replaceRevisionSegments(
        revision: TranscriptRevisionEntity,
        segments: List<TranscriptSegmentEntity>
    ) {
        upsertRevision(revision)
        deleteSegmentsForRevision(revision.id)
        insertSegments(segments)
    }
}
