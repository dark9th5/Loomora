package com.loomora.core.audio.waveform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaveformCacheStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun read(cacheKey: String): PersistedWaveform? {
        val cacheFile = cacheFile(cacheKey)
        if (!cacheFile.exists()) {
            return null
        }
        return runCatching {
            DataInputStream(cacheFile.inputStream().buffered()).use { input ->
                val sourceFingerprint = input.readUTF()
                val algorithmVersion = input.readInt()
                val resolution = input.readInt()
                val durationMs = input.readLong()
                val size = input.readInt()
                val bins = List(size) { input.readFloat() }
                PersistedWaveform(
                    sourceFingerprint = sourceFingerprint,
                    algorithmVersion = algorithmVersion,
                    resolution = resolution,
                    durationMs = durationMs,
                    bins = bins
                )
            }
        }.getOrNull()
    }

    fun write(cacheKey: String, waveform: PersistedWaveform) {
        val cacheFile = cacheFile(cacheKey)
        cacheFile.parentFile?.mkdirs()
        val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
        DataOutputStream(tempFile.outputStream().buffered()).use { output ->
            output.writeUTF(waveform.sourceFingerprint)
            output.writeInt(waveform.algorithmVersion)
            output.writeInt(waveform.resolution)
            output.writeLong(waveform.durationMs)
            output.writeInt(waveform.bins.size)
            waveform.bins.forEach(output::writeFloat)
        }
        if (!tempFile.renameTo(cacheFile)) {
            tempFile.delete()
            throw IllegalStateException("Unable to finalize waveform cache file")
        }
    }

    fun removeForFingerprint(sourceFingerprint: String) {
        cacheDir().listFiles()
            ?.filter { it.name.startsWith(sourceFingerprint) }
            ?.forEach(File::delete)
    }

    fun cacheKey(
        sourceFingerprint: String,
        algorithmVersion: Int,
        resolution: Int
    ): String = "${sourceFingerprint}_${algorithmVersion}_${resolution}"

    fun computeSourceFingerprint(sourceFile: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        sourceFile.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun cacheFile(cacheKey: String): File = File(cacheDir(), "$cacheKey.wf")

    private fun cacheDir(): File = File(context.filesDir, "waveforms")
}
