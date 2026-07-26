package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_segments",
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
        Index("orderIndex")
    ]
)
data class AudioSegmentEntity(
    @PrimaryKey
    val id: String,
    val recordingId: String,
    val orderIndex: Int,
    val startOffsetMs: Long,
    val durationMs: Long,
    val filePath: String,
    val sizeBytes: Long,
    val checksum: String,
    val finalized: Boolean,
    val createdAt: Long
)
