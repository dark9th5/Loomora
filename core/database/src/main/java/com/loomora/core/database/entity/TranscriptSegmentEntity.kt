package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptRevisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("revisionId"),
        Index("recordingId"),
        Index(value = ["revisionId", "orderIndex"], unique = true)
    ]
)
data class TranscriptSegmentEntity(
    @PrimaryKey
    val id: String,
    val revisionId: String,
    val recordingId: String,
    val orderIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val rawText: String,
    val normalizedText: String,
    val speakerLabel: String?,
    val speakerConfidence: Float?,
    val speakerIsUncertain: Boolean
)
