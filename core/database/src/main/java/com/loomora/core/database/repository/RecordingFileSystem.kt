package com.loomora.core.database.repository

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

interface RecordingFileSystem {
    fun stageForDeletion(source: File, stagingDir: File): File
    fun restoreFromStaging(stagedFile: File, destination: File)
    fun deleteIfExists(file: File): Boolean
}

@Singleton
class DefaultRecordingFileSystem @Inject constructor() : RecordingFileSystem {
    override fun stageForDeletion(source: File, stagingDir: File): File {
        if (!stagingDir.exists()) {
            stagingDir.mkdirs()
        }
        val stagedFile = File(stagingDir, "${System.currentTimeMillis()}_${source.name}")
        try {
            Files.move(
                source.toPath(),
                stagedFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: IOException) {
            Files.move(
                source.toPath(),
                stagedFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        return stagedFile
    }

    override fun restoreFromStaging(stagedFile: File, destination: File) {
        destination.parentFile?.mkdirs()
        Files.move(
            stagedFile.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    override fun deleteIfExists(file: File): Boolean {
        return !file.exists() || file.delete()
    }
}
