package com.loomora.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun migration1To3_createsOfflineAiAndTranscriptTables() {
        val dbName = "migration-test-${System.nanoTime()}.db"
        val schemaV1 = loadSchemaJson(1)
        createDatabaseFromSchema(context, dbName, schemaV1, 1)

        val migrated = Room.databaseBuilder(context, LoomoraDatabase::class.java, dbName)
            .addMigrations(LoomoraMigrations.MIGRATION_1_2, LoomoraMigrations.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val offlineModelDao = migrated.offlineModelDao()
        val analysisJobDao = migrated.analysisJobDao()
        val transcriptDao = migrated.transcriptDao()

        assertNotNull(offlineModelDao)
        assertNotNull(analysisJobDao)
        assertNotNull(transcriptDao)
        migrated.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration8To9_preservesExistingDataAndCreatesTasksTable() {
        val dbName = "migration-8-9-test-${System.nanoTime()}.db"
        val schemaV8 = loadSchemaJson(8)
        createDatabaseFromSchema(context, dbName, schemaV8, 8)

        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val rawDb = helper.writableDatabase
        rawDb.execSQL(
            """
            INSERT INTO recordings (
                id, title, createdAt, updatedAt, durationMs, status, originalFileUri, mimeType, sampleRate, channels, bitrate, sizeBytes, languageHint, isFavorite, recoveryState, transcriptStatus, insightStatus
            ) VALUES (
                'rec-mig-8-9', 'Meeting 8-9', 1000, 1000, 120000, 'SAVED', 'file:///test.m4a', 'audio/mp4', 16000, 1, 128000, 2048, 'vi', 0, 'NONE', 'COMPLETED', 'COMPLETED'
            )
            """.trimIndent()
        )
        rawDb.execSQL(
            """
            INSERT INTO transcript_revisions (
                id, recordingId, sourceFingerprint, pipelineVersion, modelId, modelVersion, status, segmentCount, createdAt, updatedAt
            ) VALUES (
                'tr-mig-8-9', 'rec-mig-8-9', 'fp123', 'v1', 'whisper-tiny', '1.0', 'COMPLETE', 1, 1000, 1000
            )
            """.trimIndent()
        )
        rawDb.execSQL(
            """
            INSERT INTO transcript_segments (
                id, revisionId, recordingId, orderIndex, startMs, endMs, rawText, normalizedText, speakerIsUncertain
            ) VALUES (
                'ts-mig-8-9', 'tr-mig-8-9', 'rec-mig-8-9', 0, 0, 5000, 'Existing Transcript Text', 'existing transcript text', 0
            )
            """.trimIndent()
        )
        rawDb.execSQL(
            """
            INSERT INTO insight_revisions (
                id, recordingId, transcriptRevisionId, sourceFingerprint, pipelineVersion, promptVersion, schemaVersion, modelId, modelVersion, kind, status, insightsJson, modelSizeBytes, loadTimeMs, generationTimeMs, generationMode, completionQuality, createdAt, updatedAt
            ) VALUES (
                'ir-mig-8-9', 'rec-mig-8-9', 'tr-mig-8-9', 'fp123', 'v1', 'p1', 's1', 'heuristic', '1.0', 'GENERATED', 'COMPLETE', '{"summary":"Existing Summary"}', 0, 0, 100, 'HEURISTIC', 'EXTRACTIVE_ONLY', 1000, 1000
            )
            """.trimIndent()
        )
        rawDb.close()
        helper.close()

        val migrated = Room.databaseBuilder(context, LoomoraDatabase::class.java, dbName)
            .addMigrations(LoomoraMigrations.MIGRATION_8_9)
            .allowMainThreadQueries()
            .build()

        val rawMigrated = migrated.openHelper.writableDatabase

        val recordingCursor = rawMigrated.query("SELECT title FROM recordings WHERE id = 'rec-mig-8-9'")
        assertTrue(recordingCursor.moveToFirst())
        assertEquals("Meeting 8-9", recordingCursor.getString(0))
        recordingCursor.close()

        val transcriptCursor = rawMigrated.query("SELECT rawText FROM transcript_segments WHERE id = 'ts-mig-8-9'")
        assertTrue(transcriptCursor.moveToFirst())
        assertEquals("Existing Transcript Text", transcriptCursor.getString(0))
        transcriptCursor.close()

        val insightCursor = rawMigrated.query("SELECT insightsJson FROM insight_revisions WHERE id = 'ir-mig-8-9'")
        assertTrue(insightCursor.moveToFirst())
        assertEquals("""{"summary":"Existing Summary"}""", insightCursor.getString(0))
        insightCursor.close()

        rawMigrated.execSQL(
            """
            INSERT INTO recording_tasks (
                id, recordingId, sourceInsightRevisionId, sourceActionIndex, title, assignee, dueDate, status, evidenceSegmentIdsCsv, sourceGenerationMode, createdAt, updatedAt, isUserEdited
            ) VALUES (
                'task-8-9', 'rec-mig-8-9', 'ir-mig-8-9', 0, 'Review Report', 'Alice', 'Friday', 'TODO', 'ts-mig-8-9', 'HEURISTIC', 1000, 1000, 0
            )
            """.trimIndent()
        )

        val taskCursor = rawMigrated.query("SELECT title, assignee, status FROM recording_tasks WHERE id = 'task-8-9'")
        assertTrue(taskCursor.moveToFirst())
        assertEquals("Review Report", taskCursor.getString(0))
        assertEquals("Alice", taskCursor.getString(1))
        assertEquals("TODO", taskCursor.getString(2))
        taskCursor.close()

        migrated.close()
        context.deleteDatabase(dbName)
    }

    private fun createDatabaseFromSchema(
        context: Context,
        dbName: String,
        schemaJson: JSONObject,
        version: Int
    ) {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val database = schemaJson.getJSONObject("database")
                        val entities = database.getJSONArray("entities")
                        for (i in 0 until entities.length()) {
                            val entity = entities.getJSONObject(i)
                            val tableName = entity.getString("tableName")
                            val createSql = entity.getString("createSql")
                                .replace("\${TABLE_NAME}", tableName)
                            db.execSQL(createSql)
                        }
                        val setupQueries = database.getJSONArray("setupQueries")
                        for (i in 0 until setupQueries.length()) {
                            db.execSQL(setupQueries.getString(i))
                        }
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()
    }

    private fun loadSchemaJson(version: Int): JSONObject {
        val rootCandidates = listOf(
            File("D:/App Android/Loomora/core/database/schemas/com.loomora.core.database.LoomoraDatabase/$version.json"),
            File("core/database/schemas/com.loomora.core.database.LoomoraDatabase/$version.json"),
            File("schemas/com.loomora.core.database.LoomoraDatabase/$version.json")
        )
        val schemaFile = rootCandidates.firstOrNull { it.exists() }
            ?: error("Could not locate schema $version.json for migration test.")
        return JSONObject(schemaFile.readText())
    }
}
