package com.loomora.core.offlineai

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
open class OfflineModelImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val compatibilityChecker: ModelCompatibilityChecker
) {
    suspend fun importModel(
        sourceUri: Uri,
        resolver: ContentResolver = context.contentResolver,
        onState: suspend (OfflineModelManifest, ModelInstallState) -> Unit = { _, _ -> }
    ): ImportedModel = withContext(Dispatchers.IO) {
        val modelsRoot = File(context.filesDir, "models").apply { mkdirs() }
        val importRoot = File(modelsRoot, "imports").apply { mkdirs() }
        val tempPackage = File(importRoot, "${System.currentTimeMillis()}.modelpack")
        val tempExtractDir = File(importRoot, "extract-${System.nanoTime()}").apply { mkdirs() }

        try {
            copyUriToFile(resolver, sourceUri, tempPackage)
            unzipPackage(tempPackage, tempExtractDir)

            val manifestFile = File(tempExtractDir, "manifest.json")
            val manifest = json.decodeFromString<OfflineModelManifest>(manifestFile.readText())
            onState(manifest, ModelInstallState.IMPORTING)
            val payloadFile = File(tempExtractDir, manifest.fileName)
            if (!payloadFile.exists() || !payloadFile.isFile) {
                throw OfflineAiException.ModelFileMissing
            }

            onState(manifest, ModelInstallState.VERIFYING)
            verifyFile(payloadFile, manifest.sizeBytes, manifest.sha256)
            manifest.additionalFiles.forEach { additional ->
                val additionalFile = File(tempExtractDir, additional.fileName)
                if (!additionalFile.exists() || !additionalFile.isFile) {
                    throw OfflineAiException.ModelFileMissing
                }
                verifyFile(additionalFile, additional.sizeBytes, additional.sha256)
            }

            val compatibility = compatibilityChecker.evaluate(manifest)
            val targetDir = File(modelsRoot, "${manifest.id}-${manifest.version}")
            val publishedPayload = File(targetDir, manifest.fileName)

            if (compatibility !is CompatibilityResult.Compatible) {
                return@withContext ImportedModel(
                    manifest = manifest,
                    publishedPayload = null,
                    compatibility = compatibility
                )
            }

            val tempPublishDir = File(importRoot, "publish-${manifest.id}-${System.nanoTime()}").apply { mkdirs() }
            tempExtractDir.copyRecursively(tempPublishDir, overwrite = true)

            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            if (!tempPublishDir.renameTo(targetDir)) {
                tempPublishDir.copyRecursively(targetDir, overwrite = true)
                tempPublishDir.deleteRecursively()
            }

            ImportedModel(
                manifest = manifest,
                publishedPayload = publishedPayload,
                compatibility = compatibility
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } finally {
            tempPackage.delete()
            tempExtractDir.deleteRecursively()
        }
    }

    protected open suspend fun copyUriToFile(
        resolver: ContentResolver,
        sourceUri: Uri,
        destination: File
    ) {
        resolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        } ?: throw OfflineAiException.ImportInterrupted
    }

    private fun unzipPackage(source: File, destinationDir: File) {
        ZipFile(source).use { zip ->
            zip.entries().iterator().forEach { entry ->
                val target = File(destinationDir, entry.name)
                val canonicalDestination = destinationDir.canonicalFile
                val canonicalTarget = target.canonicalFile
                if (!canonicalTarget.path.startsWith(canonicalDestination.path + File.separator)) {
                    throw OfflineAiException.ImportInterrupted
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun verifyFile(file: File, expectedSizeBytes: Long, expectedSha256: String) {
        if (expectedSizeBytes >= 0 && file.length() != expectedSizeBytes) {
            throw OfflineAiException.ModelChecksumMismatch
        }
        val actualSha = sha256(file)
        if (!actualSha.equals(expectedSha256, ignoreCase = true)) {
            throw OfflineAiException.ModelChecksumMismatch
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}

data class ImportedModel(
    val manifest: OfflineModelManifest,
    val publishedPayload: File?,
    val compatibility: CompatibilityResult
)
