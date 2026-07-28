package com.loomora

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.loomora.core.audio.recovery.RecordingRecoveryScanner
import com.loomora.core.offlineai.OfflineProcessingQueue
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltAndroidApp
class LoomoraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var recordingRecoveryScanner: RecordingRecoveryScanner

    @Inject
    lateinit var offlineProcessingQueue: OfflineProcessingQueue

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                recordingRecoveryScanner.scan()
                offlineProcessingQueue.reconcile()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Recovery is best-effort; dangling recordings stay in Room for the next startup scan.
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
