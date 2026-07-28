package com.loomora.core.audio.editor

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.loomora.core.model.KeepRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
@OptIn(UnstableApi::class)
class Media3AudioEditEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioEditEngine {

    override suspend fun export(
        sourceFile: File,
        keepRanges: List<KeepRange>,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()

        val sourceUri = Uri.fromFile(sourceFile)
        val editedItems = keepRanges.map { range ->
            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(range.startMs)
                        .setEndPositionMs(range.endMs)
                        .build()
                )
                .build()
            EditedMediaItem.Builder(mediaItem).build()
        }

        val sequence = EditedMediaItemSequence.Builder(editedItems).build()
        val composition = Composition.Builder(sequence).build()

        suspendCancellableCoroutine<Unit> { continuation ->
            val transformer = Transformer.Builder(context)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            continuation.resume(Unit)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            continuation.resumeWithException(exportException)
                        }
                    }
                )
                .build()

            var progressJob: Job? = null
            progressJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                val progressHolder = ProgressHolder()
                while (isActive) {
                    val progressState = transformer.getProgress(progressHolder)
                    if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        onProgress(progressHolder.progress.coerceIn(0, 100))
                    }
                    delay(100L)
                }
            }

            continuation.invokeOnCancellation {
                progressJob?.cancel()
                transformer.cancel()
                if (it is CancellationException) {
                    outputFile.delete()
                }
            }

            try {
                transformer.start(composition, outputFile.absolutePath)
            } catch (exception: Exception) {
                progressJob?.cancel()
                continuation.resumeWithException(exception)
            }
        }
    }
}
