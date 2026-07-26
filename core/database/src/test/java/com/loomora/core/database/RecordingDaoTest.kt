package com.loomora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.entity.RecordingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecordingDaoTest {

    private lateinit var database: LoomoraDatabase
    private lateinit var recordingDao: RecordingDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LoomoraDatabase::class.java
        ).allowMainThreadQueries().build()

        recordingDao = database.recordingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetRecording_returnsCorrectEntity() = runBlocking {
        val recording = createTestRecording(id = "rec-1", title = "Meeting Note")
        recordingDao.insertRecording(recording)

        val retrieved = recordingDao.getRecordingById("rec-1").first()
        assertNotNull(retrieved)
        assertEquals("Meeting Note", retrieved?.title)
        assertEquals("SAVED", retrieved?.status)
    }

    @Test
    fun getActiveRecordings_excludesSoftDeleted() = runBlocking {
        val active = createTestRecording(id = "rec-1", title = "Active Note")
        val deleted = createTestRecording(id = "rec-2", title = "Deleted Note", deletedAt = System.currentTimeMillis())

        recordingDao.insertRecording(active)
        recordingDao.insertRecording(deleted)

        val activeList = recordingDao.getActiveRecordings().first()
        assertEquals(1, activeList.size)
        assertEquals("rec-1", activeList[0].id)
    }

    @Test
    fun favoriteToggle_updatesIsFavoriteField() = runBlocking {
        val recording = createTestRecording(id = "rec-1", isFavorite = false)
        recordingDao.insertRecording(recording)

        recordingDao.setFavorite("rec-1", true, System.currentTimeMillis())

        val favorites = recordingDao.getFavoriteRecordings().first()
        assertEquals(1, favorites.size)
        assertTrue(favorites[0].isFavorite)
    }

    @Test
    fun softDeleteAndRestore_updatesDeletedAtCorrectly() = runBlocking {
        val recording = createTestRecording(id = "rec-1")
        recordingDao.insertRecording(recording)

        val now = System.currentTimeMillis()
        recordingDao.softDeleteRecording("rec-1", now, now)

        var activeList = recordingDao.getActiveRecordings().first()
        assertEquals(0, activeList.size)

        val trashedList = recordingDao.getTrashedRecordings().first()
        assertEquals(1, trashedList.size)

        recordingDao.restoreRecording("rec-1", now)
        activeList = recordingDao.getActiveRecordings().first()
        assertEquals(1, activeList.size)
    }

    @Test
    fun permanentDelete_removesEntityFromDatabase() = runBlocking {
        val recording = createTestRecording(id = "rec-1")
        recordingDao.insertRecording(recording)

        recordingDao.deleteRecordingPermanently("rec-1")

        val retrieved = recordingDao.getRecordingById("rec-1").first()
        assertNull(retrieved)
    }

    @Test
    fun searchRecordings_returnsMatchingTitlesOnly() = runBlocking {
        recordingDao.insertRecording(createTestRecording(id = "rec-1", title = "Architecture Meeting"))
        recordingDao.insertRecording(createTestRecording(id = "rec-2", title = "Grocery List"))

        val results = recordingDao.searchRecordings("Meeting").first()
        assertEquals(1, results.size)
        assertEquals("Architecture Meeting", results[0].title)
    }

    private fun createTestRecording(
        id: String,
        title: String = "Test Recording",
        isFavorite: Boolean = false,
        deletedAt: Long? = null
    ) = RecordingEntity(
        id = id,
        title = title,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        durationMs = 60000L,
        status = "SAVED",
        originalFileUri = "file:///data/user/0/com.loomora/files/$id.aac",
        editedOutputUri = null,
        mimeType = "audio/aac",
        sampleRate = 44100,
        channels = 2,
        bitrate = 128000,
        sizeBytes = 1024000L,
        languageHint = "en",
        isFavorite = isFavorite,
        deletedAt = deletedAt
    )
}
