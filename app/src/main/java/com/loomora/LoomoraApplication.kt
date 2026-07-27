package com.loomora

import android.app.Application
import com.loomora.core.audio.recovery.RecordingRecoveryScanner
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltAndroidApp
class LoomoraApplication : Application() {

    @Inject
    lateinit var recordingRecoveryScanner: RecordingRecoveryScanner

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                recordingRecoveryScanner.scan()
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
}
