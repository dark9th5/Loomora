package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insight_chunk_checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = InsightRevisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("revisionId"),
        Index(value = ["revisionId", "chunkIndex"], unique = true)
    ]
)
data class InsightChunkCheckpointEntity(
    @PrimaryKey
    val id: String,
    val revisionId: String,
    val chunkIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val segmentIdsJson: String,
    val promptVersion: String,
    val schemaVersion: String,
    val outputJson: String,
    val createdAt: Long
)
