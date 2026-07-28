package com.loomora.core.offlineai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.model.Capability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineProcessingQueueTest {
    private lateinit var context: Context
    private lateinit var database: LoomoraDatabase
    private lateinit var repository: AnalysisJobRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LoomoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnalysisJobRepository(database.analysisJobDao(), Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun duplicateEnqueue_sameLogicalKeyReturnsSameJob() = runTest {
        val first = repository.enqueueIfAbsent(
            recordingId = "rec-1",
            sourceFingerprint = "fingerprint",
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION),
            options = OfflineProcessingOptions(transcriptionModelId = "asr", insightsMode = "heuristic")
        )
        val second = repository.enqueueIfAbsent(
            recordingId = "rec-1",
            sourceFingerprint = "fingerprint",
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION),
            options = OfflineProcessingOptions(transcriptionModelId = "asr", insightsMode = "HEURISTIC")
        )

        assertEquals(first.id, second.id)
        assertEquals(1, database.analysisJobDao().observeJobsForRecording("rec-1").first().size)
    }

    @Test
    fun sourceFingerprintChangeCreatesNewLogicalJob() = runTest {
        val first = repository.enqueueIfAbsent(
            recordingId = "rec-1",
            sourceFingerprint = "fingerprint-a",
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION)
        )
        val second = repository.enqueueIfAbsent(
            recordingId = "rec-1",
            sourceFingerprint = "fingerprint-b",
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION)
        )

        assertNotEquals(first.id, second.id)
        assertEquals(2, database.analysisJobDao().observeJobsForRecording("rec-1").first().size)
    }

    @Test
    fun cancelPersistsCancelRequestedWithoutRetryState() = runTest {
        val job = repository.enqueueIfAbsent(
            recordingId = "rec-1",
            sourceFingerprint = "fingerprint",
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION)
        )

        repository.requestCancel(job.id)

        val cancelled = requireNotNull(repository.getJob(job.id))
        assertEquals(AnalysisJobStatus.CANCEL_REQUESTED.name, cancelled.status)
        assertNull(cancelled.errorCode)
    }

    @Test
    fun processRecreationReconcilesRunningBackToQueued() = runTest {
        val job = repository.enqueueIfAbsent(
            recordingId = "rec-1",
            sourceFingerprint = "fingerprint",
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION)
        )
        repository.updateState(
            jobId = job.id,
            status = AnalysisJobStatus.RUNNING,
            stage = OfflineAnalysisStage.TRANSCRIBING,
            progress = 0.5f
        )

        repository.reconcileRunningJobs()

        val reconciled = requireNotNull(repository.getJob(job.id))
        assertEquals(AnalysisJobStatus.QUEUED.name, reconciled.status)
        assertEquals(OfflineAnalysisStage.PREPARING_AUDIO.name, reconciled.stage)
    }

    @Test
    fun durableTrialReservationDoesNotDuplicateOrDoubleCommit() = runTest {
        val port = DurableTrialReservationPort(database.trialOperationDao())
        val first = port.reserve("logical", Capability.SMART_INSIGHTS)
        val second = port.reserve("logical", Capability.SMART_INSIGHTS)

        assertEquals(first.id, second.id)
        assertTrue(first.reserved)
        port.commit(first, resultRevisionId = "insight-revision-1")
        port.commit(second, resultRevisionId = "insight-revision-1")
        port.release(second)

        val operation = requireNotNull(
            database.trialOperationDao().getByLogicalJobAndCapability("logical", Capability.SMART_INSIGHTS.name)
        )
        assertEquals("COMMITTED", operation.status)
        assertEquals("insight-revision-1", operation.resultRevisionId)
        assertEquals(1, database.trialOperationDao().committedCount(Capability.SMART_INSIGHTS.name))
    }
}
