package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "background_jobs",
    indices = [
        Index("recordingId"),
        Index("state"),
        Index("idempotencyKey", unique = true)
    ]
)
data class BackgroundJobEntity(
    @PrimaryKey
    val id: String,
    val recordingId: String?,
    val type: String,
    val state: String,
    val progress: Float,
    val attempt: Int,
    val errorCode: String?,
    val idempotencyKey: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "entitlements")
data class EntitlementEntity(
    @PrimaryKey
    val id: String,
    val plan: String,
    val signedToken: String,
    val validUntil: Long,
    val offlineGraceUntil: Long,
    val lastVerifiedAt: Long,
    val source: String,
    val status: String
)

@Entity(tableName = "trial_usages")
data class TrialUsageEntity(
    @PrimaryKey
    val capability: String,
    val successfulUses: Int,
    val reservedOperationId: String?,
    val lastUpdatedAt: Long
)
