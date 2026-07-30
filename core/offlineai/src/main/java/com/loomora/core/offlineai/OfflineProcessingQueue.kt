package com.loomora.core.offlineai

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.loomora.core.database.entity.AnalysisJobEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineProcessingQueue @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analysisJobRepository: AnalysisJobRepository
) {
    fun observeJobsForRecording(recordingId: String): Flow<List<AnalysisJobEntity>> {
        return analysisJobRepository.observeJobsForRecording(recordingId)
    }

    suspend fun enqueue(
        recordingId: String,
        audioFileUri: String,
        options: OfflineProcessingOptions = OfflineProcessingOptions()
    ): AnalysisJobEntity {
        val sourceFile = File(audioFileUri.removePrefix("file://"))
        val fingerprint = if (sourceFile.exists() && sourceFile.isFile) sha256(sourceFile) else "missing:$audioFileUri"
        val job = analysisJobRepository.enqueueIfAbsent(
            recordingId = recordingId,
            sourceFingerprint = fingerprint,
            requestedCapabilities = buildSet {
                add(ModelCapability.TRANSCRIPTION)
                if (options.diarizationEnabled) add(ModelCapability.DIARIZATION)
            },
            options = options
        )
        val request = OneTimeWorkRequestBuilder<OfflineAnalysisWorker>()
            .setInputData(
                Data.Builder()
                    .putString(OfflineAnalysisWorker.KEY_JOB_ID, job.id)
                    .putString(OfflineAnalysisWorker.KEY_RECORDING_ID, recordingId)
                    .putString(OfflineAnalysisWorker.KEY_AUDIO_URI, audioFileUri)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .addTag(job.id)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(job.logicalKey, ExistingWorkPolicy.REPLACE, request)
        analysisJobRepository.updateWorkRequestId(job.id, request.id.toString())
        return job
    }

    suspend fun cancel(jobId: String) {
        analysisJobRepository.requestCancel(jobId)
        WorkManager.getInstance(context).cancelAllWorkByTag(jobId)
    }

    suspend fun reconcile() {
        val workManager = WorkManager.getInstance(context)
        analysisJobRepository.pendingJobs().forEach { job ->
            val workInfo = job.workRequestId?.let { id ->
                runCatching { workManager.getWorkInfoById(UUID.fromString(id)).get() }.getOrNull()
            }
            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.BLOCKED,
                WorkInfo.State.RUNNING -> Unit
                WorkInfo.State.CANCELLED -> analysisJobRepository.updateState(
                    jobId = job.id,
                    status = AnalysisJobStatus.CANCELLED,
                    stage = OfflineAnalysisStage.CLEANING_UP,
                    progress = job.progress
                )
                WorkInfo.State.SUCCEEDED -> markWorkerFailure(job, "WorkerFinishedWithoutResult")
                WorkInfo.State.FAILED -> markWorkerFailure(job, "WorkerFailed")
                null -> markWorkerFailure(job, "WorkRequestMissing")
            }
        }
    }

    private suspend fun markWorkerFailure(job: AnalysisJobEntity, errorCode: String) {
        analysisJobRepository.updateState(
            jobId = job.id,
            status = AnalysisJobStatus.TERMINAL_FAILURE,
            stage = OfflineAnalysisStage.CLEANING_UP,
            progress = job.progress,
            errorCode = errorCode
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "offline-analysis"
    }
}
