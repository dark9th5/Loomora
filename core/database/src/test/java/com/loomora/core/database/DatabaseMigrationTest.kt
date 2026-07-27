package com.loomora.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertNotNull
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
    private lateinit var schemaV1: JSONObject

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        schemaV1 = loadSchemaJson()
    }

    @Test
    fun migration1To3_createsOfflineAiAndTranscriptTables() {
        val dbName = "migration-test-${System.nanoTime()}.db"
        createVersion1Database(context, dbName, schemaV1)

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

    private fun createVersion1Database(
        context: Context,
        dbName: String,
        schemaJson: JSONObject
    ) {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
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

    private fun loadSchemaJson(): JSONObject {
        val rootCandidates = listOf(
            File("D:/App Android/Loomora/core/database/schemas/com.loomora.core.database.LoomoraDatabase/1.json"),
            File("core/database/schemas/com.loomora.core.database.LoomoraDatabase/1.json"),
            File("schemas/com.loomora.core.database.LoomoraDatabase/1.json")
        )
        val schemaFile = rootCandidates.firstOrNull { it.exists() }
            ?: error("Could not locate schema 1.json for migration test.")
        return JSONObject(schemaFile.readText())
    }
}
