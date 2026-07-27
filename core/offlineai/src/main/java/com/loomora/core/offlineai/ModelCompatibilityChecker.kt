package com.loomora.core.offlineai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ModelCompatibilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    open fun evaluate(manifest: OfflineModelManifest): CompatibilityResult {
        val supportedAbis = Build.SUPPORTED_ABIS.toSet()
        if (manifest.supportedAbis.intersect(supportedAbis).isEmpty()) {
            return CompatibilityResult.Incompatible(
                issue = CompatibilityIssue.ABI_UNSUPPORTED,
                detail = "Supported ABIs: ${manifest.supportedAbis.joinToString()}"
            )
        }

        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val availableRamMb = if (memoryInfo.totalMem > 0L) {
            memoryInfo.totalMem / BYTES_PER_MEGABYTE
        } else {
            activityManager?.memoryClass?.toLong() ?: 0L
        }
        if (manifest.minimumRamMb != null && availableRamMb < manifest.minimumRamMb) {
            return CompatibilityResult.Incompatible(
                issue = CompatibilityIssue.RAM_INSUFFICIENT,
                detail = "Requires ${manifest.minimumRamMb} MB RAM class, device has $availableRamMb MB."
            )
        }

        val availableStorage = StatFs(context.filesDir.absolutePath).availableBytes
        if (availableStorage < manifest.sizeBytes) {
            return CompatibilityResult.Incompatible(
                issue = CompatibilityIssue.STORAGE_INSUFFICIENT,
                detail = "Requires ${manifest.sizeBytes} bytes, available $availableStorage bytes."
            )
        }

        return CompatibilityResult.Compatible(backend = ExecutionBackend.CPU)
    }

    private companion object {
        const val BYTES_PER_MEGABYTE = 1024L * 1024L
    }
}
