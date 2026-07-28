package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loomora.core.database.entity.TrialOperationEntity

@Dao
interface TrialOperationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(operation: TrialOperationEntity): Long

    @Query("SELECT * FROM trial_operations WHERE logicalJobKey = :logicalJobKey AND capability = :capability LIMIT 1")
    suspend fun getByLogicalJobAndCapability(
        logicalJobKey: String,
        capability: String
    ): TrialOperationEntity?

    @Query("SELECT COUNT(*) FROM trial_operations WHERE capability = :capability AND status = 'COMMITTED'")
    suspend fun committedCount(capability: String): Int

    @Query("UPDATE trial_operations SET status = 'COMMITTED', committedAt = :committedAt, resultRevisionId = COALESCE(resultRevisionId, :resultRevisionId), updatedAt = :committedAt WHERE trialOperationId = :trialOperationId AND status != 'COMMITTED' AND :resultRevisionId IS NOT NULL")
    suspend fun commit(
        trialOperationId: String,
        resultRevisionId: String?,
        committedAt: Long
    ): Int

    @Query("UPDATE trial_operations SET status = 'RELEASED', releasedAt = :releasedAt, updatedAt = :releasedAt WHERE trialOperationId = :trialOperationId AND status = 'RESERVED'")
    suspend fun release(trialOperationId: String, releasedAt: Long): Int

    @Query("SELECT * FROM trial_operations WHERE status = 'RESERVED' AND resultRevisionId IS NOT NULL")
    suspend fun getPublishBeforeCommitOperations(): List<TrialOperationEntity>
}
