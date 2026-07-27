package com.loomora.core.audio.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import androidx.core.content.FileProvider
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.StorageUsageSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import android.os.StatFs
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
open class RecordingStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    open fun getStorageUsageSummary(): StorageUsageSummary {
        return StorageUsageSummary(
            recordingsBytes = directorySize(File(context.filesDir, "recordings")) { it.extension != "tmp" },
            exportsBytes = directorySize(File(context.filesDir, "exports")),
            tempBytes = directorySize(File(context.filesDir, "recordings")) { it.extension == "tmp" } +
                directorySize(File(context.filesDir, "pending_delete")),
            modelsBytes = directorySize(File(context.filesDir, "models")),
            freeBytes = StatFs(context.filesDir.absolutePath).availableBytes
        )
    }

    open fun hasAvailableBytes(requiredBytes: Long): Boolean {
        return StatFs(context.filesDir.absolutePath).availableBytes >= requiredBytes
    }

    open fun availableBytes(): Long = StatFs(context.filesDir.absolutePath).availableBytes

    open fun buildShareIntent(recording: Recording): Result<Intent> {
        val sourceFile = recording.localFile()
            ?: return Result.failure(IllegalStateException("Recording source file is missing"))
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sourceFile
        )
        return Result.success(
            Intent(Intent.ACTION_SEND).apply {
                type = recording.mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newUri(
                    context.contentResolver,
                    recording.title,
                    shareUri
                )
            }
        )
    }

    open suspend fun exportToDocument(
        recording: Recording,
        destinationUri: Uri,
        onProgress: (Int) -> Unit
    ): RecordingOperationResult = withContext(Dispatchers.IO) {
        val sourceFile = recording.localFile()
            ?: return@withContext RecordingOperationResult.SourceMissing

        if (!sourceFile.exists() || !sourceFile.isFile) {
            return@withContext RecordingOperationResult.SourceMissing
        }

        if (!hasAvailableBytes(sourceFile.length())) {
            return@withContext RecordingOperationResult.LowStorage(
                requiredBytes = sourceFile.length(),
                availableBytes = availableBytes()
            )
        }

        try {
            copyToUri(
                resolver = context.contentResolver,
                sourceFile = sourceFile,
                destinationUri = destinationUri,
                onProgress = onProgress
            )
            RecordingOperationResult.Success
        } catch (_: CancellationException) {
            RecordingOperationResult.ExportCancelled
        } catch (exception: Exception) {
            RecordingOperationResult.FileSystemFailure(
                exception.message ?: "Export failed"
            )
        }
    }

    open fun suggestedExportFileName(recording: Recording): String {
        return recording.title
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .trim()
            .ifBlank { "recording" } + ".m4a"
    }

    private fun Recording.localFile(): File? {
        val path = originalFileUri.removePrefix("file://")
        if (path.isBlank() || path.contains("..")) {
            return null
        }
        return File(path)
    }

    private fun directorySize(
        root: File,
        includeFile: (File) -> Boolean = { true }
    ): Long {
        if (!root.exists()) {
            return 0L
        }
        return root.walkTopDown()
            .filter { it.isFile && includeFile(it) }
            .sumOf { it.length() }
    }

    private suspend fun copyToUri(
        resolver: ContentResolver,
        sourceFile: File,
        destinationUri: Uri,
        onProgress: (Int) -> Unit
    ) {
        FileInputStream(sourceFile).use { input ->
            resolver.openOutputStream(destinationUri, "wt")?.use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val totalBytes = sourceFile.length().coerceAtLeast(1L)
                var copiedBytes = 0L
                onProgress(0)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) {
                        break
                    }
                    output.write(buffer, 0, read)
                    copiedBytes += read
                    onProgress(((copiedBytes * 100) / totalBytes).toInt().coerceIn(0, 100))
                }
                output.flush()
                onProgress(100)
            } ?: throw IOException("Unable to open destination output stream")
        }
    }

}
