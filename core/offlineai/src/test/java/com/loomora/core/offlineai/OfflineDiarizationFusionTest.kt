package com.loomora.core.offlineai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.SpeakerTurn
import com.loomora.core.model.TranscriptSegment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineDiarizationFusionTest {
    private lateinit var context: Context
    private lateinit var database: LoomoraDatabase
    private lateinit var repository: DiarizationRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LoomoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DiarizationRepository(database.diarizationDao(), Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun oneSpeaker_assignsGenericSpeaker() {
        val fused = TranscriptSpeakerFusion.fuse(
            transcript = listOf(TranscriptSegment(startMs = 0, endMs = 1_000, text = "Xin chao")),
            turns = listOf(SpeakerTurn(startMs = 0, endMs = 1_000, speakerLabel = "Speaker 1", speakerIndex = 0))
        )

        assertEquals(1, fused.size)
        assertEquals("Speaker 1", fused.single().speakerLabel)
        assertEquals(false, fused.single().speakerIsUncertain)
    }

    @Test
    fun twoSpeakers_splitTranscriptAtSpeakerBoundary() {
        val fused = TranscriptSpeakerFusion.fuse(
            transcript = listOf(TranscriptSegment(startMs = 0, endMs = 2_000, text = "A long sentence crossing speakers")),
            turns = listOf(
                SpeakerTurn(startMs = 0, endMs = 1_000, speakerLabel = "Speaker 1", speakerIndex = 0),
                SpeakerTurn(startMs = 1_000, endMs = 2_000, speakerLabel = "Speaker 2", speakerIndex = 1)
            )
        )

        assertEquals(listOf(0L, 1_000L), fused.map { it.startMs })
        assertEquals(listOf(1_000L, 2_000L), fused.map { it.endMs })
        assertEquals(listOf("Speaker 1", "Speaker 2"), fused.map { it.speakerLabel })
    }

    @Test
    fun shortTurns_preserveTimestampOrdering() {
        val fused = TranscriptSpeakerFusion.fuse(
            transcript = listOf(TranscriptSegment(startMs = 0, endMs = 450, text = "short turns")),
            turns = listOf(
                SpeakerTurn(startMs = 0, endMs = 150, speakerLabel = "Speaker 1", speakerIndex = 0),
                SpeakerTurn(startMs = 150, endMs = 300, speakerLabel = "Speaker 2", speakerIndex = 1),
                SpeakerTurn(startMs = 300, endMs = 450, speakerLabel = "Speaker 1", speakerIndex = 0)
            )
        )

        assertEquals(3, fused.size)
        assertEquals(fused.sortedBy { it.startMs }, fused)
    }

    @Test
    fun overlapFixture_marksUncertainMultipleSpeaker() {
        val fused = TranscriptSpeakerFusion.fuse(
            transcript = listOf(TranscriptSegment(startMs = 0, endMs = 1_000, text = "overlap")),
            turns = listOf(
                SpeakerTurn(startMs = 0, endMs = 1_000, speakerLabel = "Speaker 1", speakerIndex = 0, isOverlapped = true),
                SpeakerTurn(startMs = 0, endMs = 1_000, speakerLabel = "Speaker 2", speakerIndex = 1, isOverlapped = true)
            )
        )

        assertEquals("Speaker 1 + Speaker 2", fused.single().speakerLabel)
        assertTrue(fused.single().speakerIsUncertain)
    }

    @Test
    fun diarizationNoResult_leavesTranscriptSpeakerUnassigned() {
        val fused = TranscriptSpeakerFusion.fuse(
            transcript = listOf(TranscriptSegment(startMs = 0, endMs = 1_000, text = "no result")),
            turns = emptyList()
        )

        assertNull(fused.single().speakerLabel)
    }

    @Test
    fun retrySameDiarizationRevision_doesNotDuplicateTurns() = runTest {
        insertRecording()
        val clustering = DiarizationClusteringSettings()
        val turns = listOf(
            SpeakerTurn(startMs = 0, endMs = 1_000, speakerLabel = "Speaker 1", speakerIndex = 0)
        )

        val first = repository.publishRevision(
            recordingId = "rec-1",
            sourceFingerprint = "source",
            modelId = "diar-model",
            modelVersion = "1",
            clusteringSettings = clustering,
            turns = turns,
            processingDurationMs = 10L,
            memoryObservationKb = 100L
        )
        val second = repository.publishRevision(
            recordingId = "rec-1",
            sourceFingerprint = "source",
            modelId = "diar-model",
            modelVersion = "1",
            clusteringSettings = clustering,
            turns = turns,
            processingDurationMs = 11L,
            memoryObservationKb = 101L
        )

        assertEquals(first.id, second.id)
        assertEquals(1, database.diarizationDao().getTurnsForRevisionSync(first.id).size)
    }

    @Test
    fun manualSpeakerRename_storesAliasOnly() = runTest {
        insertRecording()

        repository.renameSpeaker("rec-1", "Speaker 1", "Interviewer")

        val aliases = repository.observeAliases("rec-1").first()
        assertEquals("Speaker 1", aliases.single().genericLabel)
        assertEquals("Interviewer", aliases.single().displayName)
    }

    @Test
    fun lowConfidenceEnrollment_remainsUnassignedWithoutExplicitConfirmation() {
        val segment = TranscriptSegment(startMs = 0, endMs = 1_000, text = "hello")

        assertNull(segment.speakerLabel)
    }

    private suspend fun insertRecording() {
        database.recordingDao().insertRecording(
            RecordingEntity(
                id = "rec-1",
                title = "Fixture",
                createdAt = 1L,
                updatedAt = 1L,
                durationMs = 1_000L,
                status = RecordingStatus.SAVED.name,
                originalFileUri = "file:///tmp/fixture.wav",
                mimeType = "audio/wav",
                sampleRate = 16_000,
                channels = 1,
                bitrate = 256_000,
                sizeBytes = 1_000L,
                languageHint = "vi"
            )
        )
    }
}
