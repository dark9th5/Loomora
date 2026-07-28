package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loomora.core.database.entity.DiarizationRevisionEntity
import com.loomora.core.database.entity.SpeakerAliasEntity
import com.loomora.core.database.entity.SpeakerTurnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiarizationDao {
    @Query("SELECT * FROM diarization_revisions WHERE recordingId = :recordingId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestRevision(recordingId: String): Flow<DiarizationRevisionEntity?>

    @Query("SELECT * FROM speaker_turns WHERE revisionId = :revisionId ORDER BY orderIndex ASC")
    fun observeTurnsForRevision(revisionId: String): Flow<List<SpeakerTurnEntity>>

    @Query("SELECT * FROM speaker_aliases WHERE recordingId = :recordingId ORDER BY genericLabel ASC")
    fun observeAliases(recordingId: String): Flow<List<SpeakerAliasEntity>>

    @Query("SELECT * FROM diarization_revisions WHERE recordingId = :recordingId AND sourceFingerprint = :sourceFingerprint AND pipelineVersion = :pipelineVersion AND modelId = :modelId AND modelVersion = :modelVersion AND clusteringSettingsHash = :clusteringSettingsHash LIMIT 1")
    suspend fun getRevisionByIdentity(
        recordingId: String,
        sourceFingerprint: String,
        pipelineVersion: String,
        modelId: String,
        modelVersion: String,
        clusteringSettingsHash: String
    ): DiarizationRevisionEntity?

    @Query("SELECT * FROM speaker_turns WHERE revisionId = :revisionId ORDER BY orderIndex ASC")
    suspend fun getTurnsForRevisionSync(revisionId: String): List<SpeakerTurnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(revision: DiarizationRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurns(turns: List<SpeakerTurnEntity>)

    @Query("DELETE FROM speaker_turns WHERE revisionId = :revisionId")
    suspend fun deleteTurnsForRevision(revisionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlias(alias: SpeakerAliasEntity)

    @Transaction
    suspend fun replaceRevisionTurns(
        revision: DiarizationRevisionEntity,
        turns: List<SpeakerTurnEntity>
    ) {
        upsertRevision(revision)
        deleteTurnsForRevision(revision.id)
        insertTurns(turns)
    }
}
