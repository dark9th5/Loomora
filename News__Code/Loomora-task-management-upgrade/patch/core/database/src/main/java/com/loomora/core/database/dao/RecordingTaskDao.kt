package com.loomora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loomora.core.database.entity.RecordingTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingTaskDao {
    @Query(
        """
        SELECT * FROM recording_tasks
        WHERE recordingId = :recordingId AND status != 'ARCHIVED'
        ORDER BY CASE WHEN status = 'DONE' THEN 1 ELSE 0 END, createdAt ASC
        """
    )
    fun observeTasksForRecording(recordingId: String): Flow<List<RecordingTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGeneratedTasks(tasks: List<RecordingTaskEntity>): List<Long>

    @Query(
        """
        UPDATE recording_tasks
        SET status = :status,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateStatus(
        taskId: String,
        status: String,
        completedAt: Long?,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE recording_tasks
        SET sourceInsightRevisionId = :sourceInsightRevisionId,
            sourceActionIndex = :sourceActionIndex,
            sourceGenerationMode = :sourceGenerationMode,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateSourceMetadata(
        taskId: String,
        sourceInsightRevisionId: String?,
        sourceActionIndex: Int?,
        sourceGenerationMode: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE recording_tasks
        SET title = :title,
            assignee = :assignee,
            dueDate = :dueDate,
            isUserEdited = 1,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateContent(
        taskId: String,
        title: String,
        assignee: String?,
        dueDate: String?,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE recording_tasks
        SET status = 'ARCHIVED', updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun archive(taskId: String, updatedAt: Long): Int
}
