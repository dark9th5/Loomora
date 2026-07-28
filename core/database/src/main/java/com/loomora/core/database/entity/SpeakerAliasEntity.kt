package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "speaker_aliases",
    primaryKeys = ["recordingId", "genericLabel"],
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordingId")]
)
data class SpeakerAliasEntity(
    val recordingId: String,
    val genericLabel: String,
    val displayName: String,
    val updatedAt: Long
)
