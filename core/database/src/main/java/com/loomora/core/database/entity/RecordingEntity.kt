package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    indices = [
        Index("createdAt"),
        Index("isFavorite"),
        Index("deletedAt")
    ]
)
data class RecordingEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val durationMs: Long,
    val status: String, // RECORDING, PAUSED, FINALIZING, SAVED, RECOVERY_FAILED
    val originalFileUri: String,
    val editedOutputUri: String? = null,
    val mimeType: String,
    val sampleRate: Int,
    val channels: Int,
    val bitrate: Int,
    val sizeBytes: Long,
    val languageHint: String = "en",
    val isFavorite: Boolean = false,
    val deletedAt: Long? = null,
    val recoveryState: String = "NORMAL",
    val transcriptStatus: String = "NOT_STARTED",
    val insightStatus: String = "NOT_STARTED"
)
