package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loomora.core.database.entity.InsightChunkCheckpointEntity
import com.loomora.core.database.entity.InsightRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insight_revisions WHERE recordingId = :recordingId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestRevision(recordingId: String): Flow<InsightRevisionEntity?>

    @Query("SELECT * FROM insight_revisions WHERE recordingId = :recordingId AND kind = 'GENERATED' ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestGeneratedRevision(recordingId: String): Flow<InsightRevisionEntity?>

    @Query("SELECT * FROM insight_revisions WHERE recordingId = :recordingId AND kind = 'USER_EDITED' ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestUserEditedRevision(recordingId: String): Flow<InsightRevisionEntity?>

    @Query("SELECT * FROM insight_revisions WHERE recordingId = :recordingId AND transcriptRevisionId = :transcriptRevisionId AND pipelineVersion = :pipelineVersion AND promptVersion = :promptVersion AND schemaVersion = :schemaVersion AND modelId = :modelId AND modelVersion = :modelVersion AND kind = :kind LIMIT 1")
    suspend fun getRevisionByIdentity(
        recordingId: String,
        transcriptRevisionId: String,
        pipelineVersion: String,
        promptVersion: String,
        schemaVersion: String,
        modelId: String,
        modelVersion: String,
        kind: String
    ): InsightRevisionEntity?

    @Query("SELECT * FROM insight_chunk_checkpoints WHERE revisionId = :revisionId ORDER BY chunkIndex ASC")
    suspend fun getCheckpointsForRevision(revisionId: String): List<InsightChunkCheckpointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(revision: InsightRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoints(checkpoints: List<InsightChunkCheckpointEntity>)

    @Query("DELETE FROM insight_chunk_checkpoints WHERE revisionId = :revisionId")
    suspend fun deleteCheckpointsForRevision(revisionId: String)

    @Transaction
    suspend fun replaceRevision(
        revision: InsightRevisionEntity,
        checkpoints: List<InsightChunkCheckpointEntity>
    ) {
        upsertRevision(revision)
        deleteCheckpointsForRevision(revision.id)
        insertCheckpoints(checkpoints)
    }
}
