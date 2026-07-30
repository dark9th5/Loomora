package com.loomora.core.offlineai

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ForegroundInfo
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

        var reservation: TrialReservation? = null
        return try {
            analysisJobRepository.updateState(
                jobId = jobId,
                status = AnalysisJobStatus.RUNNING,
                stage = OfflineAnalysisStage.PREPARING_AUDIO,
                progress = job.progress
            )
            try {
                setForeground(createForegroundInfo())
            } catch (error: IllegalStateException) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw error
                // Android 12 can reject promotion after restoring background work.
                Log.w(TAG, "Foreground promotion denied; continuing as regular work", error)
            }
            reservation = trialReservationPort.reserve(job.logicalKey)
            coordinator.processAudioForJob(
                jobId = jobId,
                recordingId = recordingId,
                audioFileUri = audioUri,
                sourceFingerprint = job.sourceFingerprint
            )
            val completedJob = analysisJobRepository.getJob(jobId)
            trialReservationPort.commit(reservation, resultRevisionId = completedJob?.stageOutputRef)
            Result.success()
        } catch (cancelled: CancellationException) {
            reservation?.let { trialReservationPort.release(it) }
            analysisJobRepository.updateState(
                jobId = jobId,
                status = AnalysisJobStatus.CANCELLED,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = job.progress
            )
            throw cancelled
        } catch (error: OfflineAiException) {
            reservation?.let { trialReservationPort.release(it) }
            val retryable = error.isRetryableForWorker()
            analysisJobRepository.updateState(
                jobId = jobId,
                status = if (retryable) AnalysisJobStatus.RETRYABLE_FAILURE else AnalysisJobStatus.TERMINAL_FAILURE,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = job.progress,
                errorCode = error::class.simpleName
            )
            if (retryable && runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        } catch (error: Throwable) {
            reservation?.let { runCatching { trialReservationPort.release(it) } }
            Log.e(TAG, "Offline analysis worker failed", error)
            analysisJobRepository.updateState(
                jobId = jobId,
                status = AnalysisJobStatus.TERMINAL_FAILURE,
                stage = OfflineAnalysisStage.CLEANING_UP,
                progress = job.progress,
                errorCode = error::class.simpleName ?: "UnexpectedWorkerFailure"
            )
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo()

    private fun createForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.offline_ai_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(applicationContext.getString(R.string.offline_ai_notification_title))
            .setContentText(applicationContext.getString(R.string.offline_ai_notification_preparing))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun OfflineAiException.isRetryableForWorker(): Boolean {
        return this == OfflineAiException.ModelInitializationFailed ||
            this == OfflineAiException.ProcessingUnavailable
    }

    companion object {
        const val KEY_JOB_ID = "jobId"
        const val KEY_RECORDING_ID = "recordingId"
        const val KEY_AUDIO_URI = "audioUri"
        private const val CHANNEL_ID = "offline_ai_processing"
        private const val NOTIFICATION_ID = 4103
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val TAG = "OfflineAnalysisWorker"
    }
}
