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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `transcript_segments` ADD COLUMN `speakerConfidence` REAL")
            database.execSQL("ALTER TABLE `transcript_segments` ADD COLUMN `speakerIsUncertain` INTEGER NOT NULL DEFAULT 0")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `diarization_revisions` (
                    `id` TEXT NOT NULL,
                    `recordingId` TEXT NOT NULL,
                    `sourceFingerprint` TEXT NOT NULL,
                    `pipelineVersion` TEXT NOT NULL,
                    `modelId` TEXT NOT NULL,
                    `modelVersion` TEXT NOT NULL,
                    `clusteringSettings` TEXT NOT NULL,
                    `clusteringSettingsHash` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `turnCount` INTEGER NOT NULL,
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
                CREATE TABLE IF NOT EXISTS `speaker_turns` (
                    `id` TEXT NOT NULL,
                    `revisionId` TEXT NOT NULL,
                    `recordingId` TEXT NOT NULL,
                    `orderIndex` INTEGER NOT NULL,
                    `startMs` INTEGER NOT NULL,
                    `endMs` INTEGER NOT NULL,
                    `speakerLabel` TEXT NOT NULL,
                    `speakerIndex` INTEGER NOT NULL,
                    `confidence` REAL,
                    `isOverlapped` INTEGER NOT NULL,
                    `isUncertain` INTEGER NOT NULL,
                    `alternateSpeakerLabelsJson` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`revisionId`) REFERENCES `diarization_revisions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `speaker_aliases` (
                    `recordingId` TEXT NOT NULL,
                    `genericLabel` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`recordingId`, `genericLabel`),
                    FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_diarization_revisions_recordingId` ON `diarization_revisions` (`recordingId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_diarization_revisions_recordingId_sourceFingerprint_pipelineVersion_modelId_modelVersion_clusteringSettingsHash` ON `diarization_revisions` (`recordingId`, `sourceFingerprint`, `pipelineVersion`, `modelId`, `modelVersion`, `clusteringSettingsHash`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_speaker_turns_revisionId` ON `speaker_turns` (`revisionId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_speaker_turns_recordingId` ON `speaker_turns` (`recordingId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_speaker_turns_revisionId_orderIndex` ON `speaker_turns` (`revisionId`, `orderIndex`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_speaker_aliases_recordingId` ON `speaker_aliases` (`recordingId`)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `insight_revisions` (
                    `id` TEXT NOT NULL,
                    `recordingId` TEXT NOT NULL,
                    `transcriptRevisionId` TEXT NOT NULL,
                    `sourceFingerprint` TEXT NOT NULL,
                    `pipelineVersion` TEXT NOT NULL,
                    `promptVersion` TEXT NOT NULL,
                    `schemaVersion` TEXT NOT NULL,
                    `modelId` TEXT NOT NULL,
                    `modelVersion` TEXT NOT NULL,
                    `languageTag` TEXT,
                    `kind` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `insightsJson` TEXT NOT NULL,
                    `modelSizeBytes` INTEGER NOT NULL,
                    `loadTimeMs` INTEGER NOT NULL,
                    `generationTimeMs` INTEGER NOT NULL,
                    `memoryObservationKb` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`transcriptRevisionId`) REFERENCES `transcript_revisions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `insight_chunk_checkpoints` (
                    `id` TEXT NOT NULL,
                    `revisionId` TEXT NOT NULL,
                    `chunkIndex` INTEGER NOT NULL,
                    `startMs` INTEGER NOT NULL,
                    `endMs` INTEGER NOT NULL,
                    `segmentIdsJson` TEXT NOT NULL,
                    `promptVersion` TEXT NOT NULL,
                    `schemaVersion` TEXT NOT NULL,
                    `outputJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`revisionId`) REFERENCES `insight_revisions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_insight_revisions_recordingId` ON `insight_revisions` (`recordingId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_insight_revisions_transcriptRevisionId` ON `insight_revisions` (`transcriptRevisionId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_insight_revisions_recordingId_transcriptRevisionId_pipelineVersion_promptVersion_schemaVersion_modelId_modelVersion_kind` ON `insight_revisions` (`recordingId`, `transcriptRevisionId`, `pipelineVersion`, `promptVersion`, `schemaVersion`, `modelId`, `modelVersion`, `kind`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_insight_chunk_checkpoints_revisionId` ON `insight_chunk_checkpoints` (`revisionId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_insight_chunk_checkpoints_revisionId_chunkIndex` ON `insight_chunk_checkpoints` (`revisionId`, `chunkIndex`)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `stage` TEXT NOT NULL DEFAULT 'QUEUED'")
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `workRequestId` TEXT")
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `checkpointRef` TEXT")
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `skipReason` TEXT")
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `fallbackReason` TEXT")
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `startedAt` INTEGER")
            database.execSQL("ALTER TABLE `analysis_jobs` ADD COLUMN `finishedAt` INTEGER")
            database.execSQL("ALTER TABLE `insight_revisions` ADD COLUMN `generationMode` TEXT NOT NULL DEFAULT 'HEURISTIC'")
            database.execSQL("ALTER TABLE `insight_revisions` ADD COLUMN `completionQuality` TEXT NOT NULL DEFAULT 'EXTRACTIVE_ONLY'")
            database.execSQL("ALTER TABLE `insight_revisions` ADD COLUMN `fallbackReason` TEXT")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `trial_operations` (
                    `trialOperationId` TEXT NOT NULL,
                    `logicalJobKey` TEXT NOT NULL,
                    `capability` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `reservedAt` INTEGER NOT NULL,
                    `committedAt` INTEGER,
                    `releasedAt` INTEGER,
                    `resultRevisionId` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`trialOperationId`)
                )
                """.trimIndent()
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_trial_operations_logicalJobKey_capability` ON `trial_operations` (`logicalJobKey`, `capability`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_trial_operations_status` ON `trial_operations` (`status`)")
        }
    }
}
