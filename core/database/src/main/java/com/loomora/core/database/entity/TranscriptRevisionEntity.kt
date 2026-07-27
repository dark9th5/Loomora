package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_revisions",
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
        Index(value = ["recordingId", "sourceFingerprint", "pipelineVersion", "modelId", "modelVersion"], unique = true)
    ]
)
data class TranscriptRevisionEntity(
    @PrimaryKey
    val id: String,
    val recordingId: String,
    val sourceFingerprint: String,
    val pipelineVersion: String,
    val modelId: String,
    val modelVersion: String,
    val languageTag: String?,
    val status: String,
    val segmentCount: Int,
    val processingDurationMs: Long?,
    val memoryObservationKb: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
