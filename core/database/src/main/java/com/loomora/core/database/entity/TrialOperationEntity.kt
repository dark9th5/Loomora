package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trial_operations",
    indices = [
        Index(value = ["logicalJobKey", "capability"], unique = true),
        Index("status")
    ]
)
data class TrialOperationEntity(
    @PrimaryKey
    val trialOperationId: String,
    val logicalJobKey: String,
    val capability: String,
    val status: String,
    val reservedAt: Long,
    val committedAt: Long?,
    val releasedAt: Long?,
    val resultRevisionId: String?,
    val updatedAt: Long
)
