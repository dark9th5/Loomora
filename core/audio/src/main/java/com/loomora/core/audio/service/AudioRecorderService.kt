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
import com.loomora.core.audio.engine.AudioRecordEngine
import com.loomora.core.audio.model.RecorderState
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecorderService : Service() {

    @Inject
    lateinit var audioRecordEngine: AudioRecordEngine

    @Inject
    lateinit var recordingRepository: RecordingRepository

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

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
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "New Recording"
                val id = audioRecordEngine.startRecording(this, title)
                if (id.isNotEmpty()) {
                    startForeground(NOTIFICATION_ID, createNotification("Recording", 0L, isPaused = false))
                }
            }
            ACTION_PAUSE -> audioRecordEngine.pauseRecording()
            ACTION_RESUME -> audioRecordEngine.resumeRecording()
            ACTION_STOP -> stopAndSaveRecording()
        }
        return START_NOT_STICKY
    }

    private fun observeRecorderState() {
        serviceScope.launch {
            audioRecordEngine.state.collectLatest { state ->
                when (state) {
                    is RecorderState.Recording -> {
                        val notification = createNotification("Recording", state.durationMs, isPaused = false)
                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is RecorderState.Paused -> {
                        val notification = createNotification("Paused", state.durationMs, isPaused = true)
                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is RecorderState.Completed -> {
                        saveRecordingToRepository(state.recordingId, state.fileUri)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    is RecorderState.Error -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopAndSaveRecording() {
        val file = audioRecordEngine.stopRecording()
        if (file == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun saveRecordingToRepository(recordingId: String, filePath: String) {
        val file = File(filePath)
        val durationMs = audioRecordEngine.getCurrentDurationMs()
        val recording = Recording(
            id = recordingId,
            title = "Recording ${recordingId.take(6)}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            durationMs = durationMs,
            status = RecordingStatus.SAVED,
            originalFileUri = "file://${file.absolutePath}",
            mimeType = "audio/aac",
            sampleRate = 44100,
            channels = 2,
            bitrate = 128000,
            sizeBytes = if (file.exists()) file.length() else 0L
        )

        serviceScope.launch(Dispatchers.IO) {
            recordingRepository.insertRecording(recording)
        }
    }

    private fun createNotification(statusText: String, durationMs: Long, isPaused: Boolean): Notification {
        val seconds = durationMs / 1000
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)

        val pauseResumeAction = if (isPaused) {
            val intent = Intent(this, AudioRecorderService::class.java).apply { action = ACTION_RESUME }
            val pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(0, "Resume", pendingIntent).build()
        } else {
            val intent = Intent(this, AudioRecorderService::class.java).apply { action = ACTION_PAUSE }
            val pendingIntent = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(0, "Pause", pendingIntent).build()
        }

        val stopIntent = Intent(this, AudioRecorderService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action.Builder(0, "Stop", stopPendingIntent).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Loomora - $statusText")
            .setContentText("Duration: $timeString")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Loomora Recording Channel",
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

        fun startService(context: Context, title: String = "New Recording") {
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
    }
}
