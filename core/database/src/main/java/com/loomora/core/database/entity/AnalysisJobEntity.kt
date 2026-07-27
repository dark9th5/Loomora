package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_jobs",
    indices = [
        Index("recordingId"),
        Index("status"),
        Index(value = ["logicalKey"], unique = true)
    ]
)
data class AnalysisJobEntity(
    @PrimaryKey
    val id: String,
    val logicalKey: String,
    val recordingId: String,
    val sourceFingerprint: String,
    val pipelineVersion: String,
    val requestedOptionsJson: String,
    val status: String,
    val progress: Float,
    val attempt: Int,
    val stageOutputRef: String?,
    val modelVersionsJson: String,
    val errorCode: String?,
    val createdAt: Long,
    val updatedAt: Long
)
