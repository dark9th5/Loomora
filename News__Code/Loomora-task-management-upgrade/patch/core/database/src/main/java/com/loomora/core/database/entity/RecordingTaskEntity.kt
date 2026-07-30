package com.loomora.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recording_tasks",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recordingId"),
        Index("status"),
        Index("sourceInsightRevisionId")
    ]
)
data class RecordingTaskEntity(
    @PrimaryKey
    val id: String,
    val recordingId: String,
    val sourceInsightRevisionId: String? = null,
    val sourceActionIndex: Int? = null,
    val title: String,
    val assignee: String? = null,
    val dueDate: String? = null,
    @ColumnInfo(defaultValue = "'TODO'")
    val status: String = STATUS_TODO,
    @ColumnInfo(defaultValue = "''")
    val evidenceSegmentIdsCsv: String = "",
    @ColumnInfo(defaultValue = "'UNKNOWN'")
    val sourceGenerationMode: String = "UNKNOWN",
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isUserEdited: Boolean = false
) {
    val isCompleted: Boolean
        get() = status == STATUS_DONE

    companion object {
        const val STATUS_TODO = "TODO"
        const val STATUS_DONE = "DONE"
        const val STATUS_ARCHIVED = "ARCHIVED"
    }
}
