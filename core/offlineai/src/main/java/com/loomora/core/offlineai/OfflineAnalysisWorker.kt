package com.loomora.core.offlineai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class OfflineAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coordinator: OfflineAnalysisCoordinator,
    private val analysisJobRepository: AnalysisJobRepository,
    private val trialReservationPort: TrialReservationPort
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val recordingId = inputData.getString(KEY_RECORDING_ID) ?: return Result.failure()
        val audioUri = inputData.getString(KEY_AUDIO_URI) ?: return Result.failure()
        val job = analysisJobRepository.getJob(jobId) ?: return Result.failure()
        if (job.status == AnalysisJobStatus.CANCEL_REQUESTED.name) {
            analysisJobRepository.updateState(
                jobId = jobId,
                status = AnalysisJobStatus.CANCELLED,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = job.progress
            )
            return Result.success()
        }

        val reservation = trialReservationPort.reserve(job.logicalKey)
        return try {
            analysisJobRepository.updateState(
                jobId = jobId,
                status = AnalysisJobStatus.RUNNING,
                stage = OfflineAnalysisStage.PREPARING_AUDIO,
                progress = job.progress
            )
            coordinator.processAudioForJob(
                jobId = jobId,
                recordingId = recordingId,
                audioFileUri = audioUri
            )
            val completedJob = analysisJobRepository.getJob(jobId)
            trialReservationPort.commit(reservation, resultRevisionId = completedJob?.stageOutputRef)
            Result.success()
        } catch (cancelled: CancellationException) {
            trialReservationPort.release(reservation)
            analysisJobRepository.updateState(
                jobId = jobId,
                status = AnalysisJobStatus.CANCELLED,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = job.progress
            )
            throw cancelled
        } catch (error: OfflineAiException) {
            trialReservationPort.release(reservation)
            val retryable = error.isRetryableForWorker()
            analysisJobRepository.updateState(
                jobId = jobId,
                status = if (retryable) AnalysisJobStatus.RETRYABLE_FAILURE else AnalysisJobStatus.TERMINAL_FAILURE,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = job.progress,
                errorCode = error::class.simpleName
            )
            if (retryable && runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun OfflineAiException.isRetryableForWorker(): Boolean {
        return this == OfflineAiException.ModelInitializationFailed ||
            this == OfflineAiException.ProcessingUnavailable
    }

    companion object {
        const val KEY_JOB_ID = "jobId"
        const val KEY_RECORDING_ID = "recordingId"
        const val KEY_AUDIO_URI = "audioUri"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
