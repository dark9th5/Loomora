package com.loomora.core.offlineai

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineModelAutoInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val catalog: DefaultOfflineModelCatalog,
    private val repository: OfflineModelRepository,
    private val json: Json
) {
    suspend fun installTranscriptionModel(onProgress: (Int) -> Unit = {}) =
        installModel(DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID, onProgress)

    suspend fun installRecommendedModels(onProgress: (Int) -> Unit = {}) {
        val modelIds = listOf(
            DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID,
            DefaultOfflineModelCatalog.RECOMMENDED_VAD_MODEL_ID,
            DefaultOfflineModelCatalog.RECOMMENDED_DIARIZATION_MODEL_ID
        )
        modelIds.forEachIndexed { index, modelId ->
            installModel(modelId) { modelProgress ->
                onProgress(((index * 100 + modelProgress) / modelIds.size).coerceIn(0, 100))
            }
        }
    }

    suspend fun installModel(
        modelId: String,
        onProgress: (Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (repository.getModelDetails(modelId)?.state == ModelInstallState.READY) {
            onProgress(100)
            return@withContext
        }
        val manifest = catalog.manifests.first { it.id == modelId }
        val files = listOf(
            OfflineModelFile(manifest.fileName, manifest.sizeBytes, manifest.sha256)
        ) + manifest.additionalFiles
        val workDir = File(context.cacheDir, "model-download/${manifest.id}").apply {
            deleteRecursively()
            mkdirs()
        }
        val pack = File(workDir, "${manifest.id}.zip")
        try {
            ZipOutputStream(BufferedOutputStream(pack.outputStream())).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(json.encodeToString(manifest).toByteArray())
                zip.closeEntry()

                var completedBytes = 0L
                val totalBytes = files.sumOf { it.sizeBytes.coerceAtLeast(0L) }.coerceAtLeast(1L)
                files.forEach { modelFile ->
                    zip.putNextEntry(ZipEntry(modelFile.fileName))
                    download(downloadUrl(manifest, modelFile)) { bytes ->
                        onProgress((((completedBytes + bytes) * 100L) / totalBytes).toInt().coerceIn(0, 99))
                    }.use { input -> input.copyTo(zip, DEFAULT_BUFFER_SIZE) }
                    zip.closeEntry()
                    completedBytes += modelFile.sizeBytes.coerceAtLeast(0L)
                }
            }
            repository.importModel(Uri.fromFile(pack))
            onProgress(100)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun downloadUrl(manifest: OfflineModelManifest, file: OfflineModelFile): String {
        return when (manifest.capability) {
            ModelCapability.TRANSCRIPTION -> requireNotNull(manifest.sourceUrl).trimEnd('/') + "/resolve/main/" + file.fileName
            ModelCapability.VOICE_ACTIVITY_DETECTION -> requireNotNull(manifest.sourceUrl).trimEnd('/') + "/" + file.fileName
            ModelCapability.DIARIZATION -> when {
                file.fileName.contains("segmentation", ignoreCase = true) -> PYANNOTE_INT8_URL
                else -> SPEAKER_EMBEDDING_BASE_URL + file.fileName
            }
            else -> error("No automatic download source for ${manifest.capability}")
        }
    }

    private fun download(url: String, onBytesRead: (Long) -> Unit): BufferedInputStream {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Loomora-Android")
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            error("Model download failed with HTTP ${connection.responseCode}")
        }
        var bytesRead = 0L
        return object : BufferedInputStream(connection.inputStream) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                return super.read(buffer, offset, length).also { count ->
                    if (count > 0) {
                        bytesRead += count
                        onBytesRead(bytesRead)
                    }
                }
            }

            override fun close() {
                super.close()
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val PYANNOTE_INT8_URL =
            "https://huggingface.co/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0/resolve/main/model.int8.onnx"
        const val SPEAKER_EMBEDDING_BASE_URL =
            "https://huggingface.co/csukuangfj/speaker-embedding-models/resolve/main/"
    }
}
