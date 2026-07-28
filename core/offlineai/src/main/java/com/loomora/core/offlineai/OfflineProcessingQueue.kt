package com.loomora.core.offlineai

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.loomora.core.database.entity.AnalysisJobEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineProcessingQueue @Inject constructor(
    @ApplicationContext private val context: Context,
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
            requestedCapabilities = setOf(ModelCapability.TRANSCRIPTION),
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
            .enqueueUniqueWork(job.logicalKey, ExistingWorkPolicy.KEEP, request)
        analysisJobRepository.updateWorkRequestId(job.id, request.id.toString())
        return job
    }

    suspend fun cancel(jobId: String) {
        analysisJobRepository.requestCancel(jobId)
        WorkManager.getInstance(context).cancelAllWorkByTag(jobId)
    }

    suspend fun reconcile() {
        analysisJobRepository.reconcileRunningJobs()
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
