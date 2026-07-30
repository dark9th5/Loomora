package com.loomora.core.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.loomora.core.audio.R
import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.audio.enhance.RnnoiseAudioEnhancer
import com.loomora.core.audio.model.RecorderState
import com.loomora.core.database.dao.RecordingDao
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import com.loomora.core.datastore.NoiseReductionLevel
import com.loomora.core.model.RecordingStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecorderService : Service() {

    @Inject
    lateinit var audioRecordEngine: AudioRecordEngine

    @Inject
    lateinit var recordingDao: RecordingDao

    @Inject
    lateinit var preferencesDataSource: LoomoraPreferencesDataSource

    @Inject
    lateinit var audioEnhancer: RnnoiseAudioEnhancer

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var activeRecordingId: String? = null
    private var activeTitle: String? = null
    private var activeNoiseReductionLevel: NoiseReductionLevel = NoiseReductionLevel.LIGHT

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecorderService = this@AudioRecorderService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeRecorderState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.audio_recording_default_title)
                startRecordingSession(title)
            }
            ACTION_PAUSE -> if (isCurrentSession(intent)) pauseRecordingSession()
            ACTION_RESUME -> if (isCurrentSession(intent)) resumeRecordingSession()
            ACTION_STOP -> if (isCurrentSession(intent)) stopAndSaveRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession(title: String) {
        val currentState = audioRecordEngine.state.value
        if (currentState is RecorderState.Recording || currentState is RecorderState.Paused || activeRecordingId != null) {
            return
        }

        val recordingId = UUID.randomUUID().toString()
        activeRecordingId = recordingId
        activeTitle = title
        val outputFile = File(File(filesDir, "recordings"), "$recordingId.m4a")
        val now = System.currentTimeMillis()
        val recording = RecordingEntity(
            id = recordingId,
            title = title,
            createdAt = now,
            updatedAt = now,
            durationMs = 0L,
            status = RecordingStatus.RECORDING.name,
            originalFileUri = "file://${outputFile.absolutePath}",
            editedOutputUri = null,
            mimeType = "audio/aac",
            sampleRate = 44100,
            channels = 2,
            bitrate = 128000,
            sizeBytes = 0L
        )

        serviceScope.launch(Dispatchers.IO) {
            recordingDao.insertRecording(recording)
                val preferences = preferencesDataSource.userPreferences.first()
                val audioSource = preferences.recordingAudioSource
                activeNoiseReductionLevel = preferences.noiseReductionLevel
                val started = audioRecordEngine.startRecording(
                    this@AudioRecorderService,
                    recordingId,
                    outputFile,
                    audioSource
                )
                if (started) {
                    launch(Dispatchers.Main) {
	                    startForeground(NOTIFICATION_ID, createNotification(R.string.audio_notification_status_recording, 0L, isPaused = false, recordingId = recordingId))
	                }
            } else {
                outputFile.delete()
                recordingDao.updateRecordingStatus(
                    id = recordingId,
                    status = RecordingStatus.RECOVERY_FAILED.name,
                    durationMs = 0L,
                    updatedAt = System.currentTimeMillis()
                )
                activeRecordingId = null
                activeTitle = null
                activeNoiseReductionLevel = NoiseReductionLevel.LIGHT
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        audioRecordEngine.release()
        super.onDestroy()
    }

    private fun observeRecorderState() {
        serviceScope.launch {
            audioRecordEngine.state.collectLatest { state ->
                when (state) {
	                    is RecorderState.Recording -> {
	                        updateRecordingStatus(state.recordingId, RecordingStatus.RECORDING, state.durationMs)
	                        val notification = createNotification(R.string.audio_notification_status_recording, state.durationMs, isPaused = false, recordingId = state.recordingId)
	                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	                        manager.notify(NOTIFICATION_ID, notification)
	                    }
	                    is RecorderState.Paused -> {
	                        updateRecordingStatus(state.recordingId, RecordingStatus.PAUSED, state.durationMs)
	                        val notification = createNotification(R.string.audio_notification_status_paused, state.durationMs, isPaused = true, recordingId = state.recordingId)
	                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	                        manager.notify(NOTIFICATION_ID, notification)
	                    }
		                    is RecorderState.Error -> {
		                        activeRecordingId?.let { id ->
		                            updateRecordingStatus(id, RecordingStatus.RECOVERY_FAILED, audioRecordEngine.getCurrentDurationMs())
		                        }
	                        stopForeground(STOP_FOREGROUND_REMOVE)
	                        stopSelf()
	                    }
                    else -> {}
                }
            }
        }
    }

    private fun pauseRecordingSession() {
        audioRecordEngine.pauseRecording()
    }

    private fun resumeRecordingSession() {
        audioRecordEngine.resumeRecording()
    }

    private fun updateRecordingStatus(recordingId: String, status: RecordingStatus, durationMs: Long) {
        serviceScope.launch(Dispatchers.IO) {
            recordingDao.updateRecordingStatus(recordingId, status.name, durationMs, System.currentTimeMillis())
        }
    }

    private fun stopAndSaveRecording() {
        val result = audioRecordEngine.stopRecording()
        if (result == null) {
            val failedRecordingId = activeRecordingId
            val durationMs = audioRecordEngine.getCurrentDurationMs()
            if (failedRecordingId != null) {
                val failedFilePath = File(File(filesDir, "recordings"), "$failedRecordingId.m4a").absolutePath
                serviceScope.launch(Dispatchers.IO) {
                    recordingDao.updateRecordingStatus(
                        id = failedRecordingId,
                        status = RecordingStatus.RECOVERY_FAILED.name,
                        durationMs = durationMs,
                        updatedAt = System.currentTimeMillis()
                    )
                    launch(Dispatchers.Main) {
                        audioRecordEngine.markSaveFailed(failedRecordingId, failedFilePath, durationMs)
                    }
                }
            }
            activeRecordingId = null
            activeTitle = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            saveCompletedRecording(result.recordingId, result.file.absolutePath, result.durationMs)
        }
    }

    private fun saveCompletedRecording(recordingId: String, filePath: String, durationMs: Long) {
        val file = File(filePath)

        serviceScope.launch(Dispatchers.IO) {
            val fileUri = "file://${file.absolutePath}"
            try {
                val existing = recordingDao.getRecordingByIdSync(recordingId)
                val now = System.currentTimeMillis()
                val filteredFile = audioEnhancer.enhance(
                    source = file,
                    destination = File(File(filesDir, "recordings/filtered"), "$recordingId-denoised.wav"),
                    level = activeNoiseReductionLevel
                )
                recordingDao.insertRecording(
                    (existing ?: RecordingEntity(
                        id = recordingId,
	                        title = activeTitle ?: getString(R.string.audio_recording_recovery_title, recordingId.take(6)),
                        createdAt = now,
                        updatedAt = now,
                        durationMs = 0L,
                        status = RecordingStatus.FINALIZING.name,
                        originalFileUri = fileUri,
                        editedOutputUri = null,
                        mimeType = "audio/aac",
                        sampleRate = 44100,
                        channels = 2,
                        bitrate = 128000,
                        sizeBytes = 0L
                    )).copy(
                        updatedAt = now,
                        durationMs = durationMs,
                        status = RecordingStatus.SAVED.name,
                        originalFileUri = fileUri,
                        editedOutputUri = filteredFile?.let { "file://${it.absolutePath}" },
                        sizeBytes = if (file.exists()) file.length() else 0L
                    )
                )
                activeRecordingId = null
                activeTitle = null
                activeNoiseReductionLevel = NoiseReductionLevel.LIGHT
                launch(Dispatchers.Main) {
                    audioRecordEngine.markSaved(recordingId, file.absolutePath, durationMs)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    audioRecordEngine.markSaveFailed(recordingId, file.absolutePath, durationMs)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }

    private fun createNotification(statusTextResId: Int, durationMs: Long, isPaused: Boolean, recordingId: String?): Notification {
        val seconds = durationMs / 1000
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
        val statusText = getString(statusTextResId)

		        val pauseResumeAction = if (isPaused) {
		            val intent = Intent(this, AudioRecorderService::class.java).apply {
		                action = ACTION_RESUME
		                putExtra(EXTRA_RECORDING_ID, recordingId)
		            }
		            val pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
		            NotificationCompat.Action.Builder(0, getString(R.string.audio_notification_action_resume), pendingIntent).build()
		        } else {
		            val intent = Intent(this, AudioRecorderService::class.java).apply {
		                action = ACTION_PAUSE
		                putExtra(EXTRA_RECORDING_ID, recordingId)
		            }
		            val pendingIntent = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE)
		            NotificationCompat.Action.Builder(0, getString(R.string.audio_notification_action_pause), pendingIntent).build()
		        }
	
	        val stopIntent = Intent(this, AudioRecorderService::class.java).apply {
	            action = ACTION_STOP
	            putExtra(EXTRA_RECORDING_ID, recordingId)
	        }
        val stopPendingIntent = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action.Builder(0, getString(R.string.audio_notification_action_stop), stopPendingIntent).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.audio_notification_title, statusText))
            .setContentText(getString(R.string.audio_notification_duration, timeString))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun isCurrentSession(intent: Intent): Boolean {
        val commandRecordingId = intent.getStringExtra(EXTRA_RECORDING_ID)
        return commandRecordingId == null || commandRecordingId == activeRecordingId
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	            val channel = NotificationChannel(
	                CHANNEL_ID,
	                getString(R.string.audio_notification_channel_name),
	                NotificationManager.IMPORTANCE_LOW
	            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "loomora_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.loomora.action.START_RECORDING"
        const val ACTION_PAUSE = "com.loomora.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.loomora.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.loomora.action.STOP_RECORDING"

        const val EXTRA_TITLE = "extra_recording_title"
        const val EXTRA_RECORDING_ID = "extra_recording_id"

        fun startService(context: Context, title: String) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun pauseService(context: Context, recordingId: String?) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_RECORDING_ID, recordingId)
            }
            context.startService(intent)
        }

        fun resumeService(context: Context, recordingId: String?) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_RECORDING_ID, recordingId)
            }
            context.startService(intent)
        }

        fun stopService(context: Context, recordingId: String?) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_RECORDING_ID, recordingId)
            }
            context.startService(intent)
        }
    }
}
