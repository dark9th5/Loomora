package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insight_revisions",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TranscriptRevisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptRevisionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recordingId"),
        Index("transcriptRevisionId"),
        Index(value = ["recordingId", "transcriptRevisionId", "pipelineVersion", "promptVersion", "schemaVersion", "modelId", "modelVersion", "kind"], unique = true)
    ]
)
data class InsightRevisionEntity(
    @PrimaryKey
    val id: String,
    val recordingId: String,
    val transcriptRevisionId: String,
    val sourceFingerprint: String,
    val pipelineVersion: String,
    val promptVersion: String,
    val schemaVersion: String,
    val modelId: String,
    val modelVersion: String,
    val languageTag: String?,
    val kind: String,
    val status: String,
    val insightsJson: String,
    val modelSizeBytes: Long,
    val loadTimeMs: Long,
    val generationTimeMs: Long,
    val memoryObservationKb: Long?,
    val generationMode: String,
    val completionQuality: String,
    val fallbackReason: String?,
    val createdAt: Long,
    val updatedAt: Long
)
