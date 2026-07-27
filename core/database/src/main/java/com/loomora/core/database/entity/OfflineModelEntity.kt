package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_models")
data class OfflineModelEntity(
    @PrimaryKey
    val modelId: String,
    val version: String,
    val capability: String,
    val runtime: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val minimumRamMb: Int?,
    val supportedAbisJson: String,
    val supportedLanguagesJson: String,
    val licenseName: String,
    val licenseUrl: String?,
    val sourceUrl: String?,
    val pipelineCompatibility: String,
    val state: String,
    val installedPath: String?,
    val installedAt: Long?,
    val lastVerifiedAt: Long?,
    val errorCode: String?
)
