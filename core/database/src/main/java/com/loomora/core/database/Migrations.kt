package com.loomora.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object LoomoraMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `offline_models` (
                    `modelId` TEXT NOT NULL,
                    `version` TEXT NOT NULL,
                    `capability` TEXT NOT NULL,
                    `runtime` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `sha256` TEXT NOT NULL,
                    `minimumRamMb` INTEGER,
                    `supportedAbisJson` TEXT NOT NULL,
                    `supportedLanguagesJson` TEXT NOT NULL,
                    `licenseName` TEXT NOT NULL,
                    `licenseUrl` TEXT,
                    `sourceUrl` TEXT,
                    `pipelineCompatibility` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `installedPath` TEXT,
                    `installedAt` INTEGER,
                    `lastVerifiedAt` INTEGER,
                    `errorCode` TEXT,
                    PRIMARY KEY(`modelId`)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `analysis_jobs` (
                    `id` TEXT NOT NULL,
                    `logicalKey` TEXT NOT NULL,
                    `recordingId` TEXT NOT NULL,
                    `sourceFingerprint` TEXT NOT NULL,
                    `pipelineVersion` TEXT NOT NULL,
                    `requestedOptionsJson` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `progress` REAL NOT NULL,
                    `attempt` INTEGER NOT NULL,
                    `stageOutputRef` TEXT,
                    `modelVersionsJson` TEXT NOT NULL,
                    `errorCode` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_analysis_jobs_recordingId` ON `analysis_jobs` (`recordingId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_analysis_jobs_status` ON `analysis_jobs` (`status`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_analysis_jobs_logicalKey` ON `analysis_jobs` (`logicalKey`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transcript_revisions` (
                    `id` TEXT NOT NULL,
                    `recordingId` TEXT NOT NULL,
                    `sourceFingerprint` TEXT NOT NULL,
                    `pipelineVersion` TEXT NOT NULL,
                    `modelId` TEXT NOT NULL,
                    `modelVersion` TEXT NOT NULL,
                    `languageTag` TEXT,
                    `status` TEXT NOT NULL,
                    `segmentCount` INTEGER NOT NULL,
                    `processingDurationMs` INTEGER,
                    `memoryObservationKb` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transcript_segments` (
                    `id` TEXT NOT NULL,
                    `revisionId` TEXT NOT NULL,
                    `recordingId` TEXT NOT NULL,
                    `orderIndex` INTEGER NOT NULL,
                    `startMs` INTEGER NOT NULL,
                    `endMs` INTEGER NOT NULL,
                    `rawText` TEXT NOT NULL,
                    `normalizedText` TEXT NOT NULL,
                    `speakerLabel` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`revisionId`) REFERENCES `transcript_revisions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_revisions_recordingId` ON `transcript_revisions` (`recordingId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transcript_revisions_recordingId_sourceFingerprint_pipelineVersion_modelId_modelVersion` ON `transcript_revisions` (`recordingId`, `sourceFingerprint`, `pipelineVersion`, `modelId`, `modelVersion`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_segments_revisionId` ON `transcript_segments` (`revisionId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_segments_recordingId` ON `transcript_segments` (`recordingId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transcript_segments_revisionId_orderIndex` ON `transcript_segments` (`revisionId`, `orderIndex`)")
        }
    }
}
