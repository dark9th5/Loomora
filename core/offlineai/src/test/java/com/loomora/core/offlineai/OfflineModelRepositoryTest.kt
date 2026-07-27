package com.loomora.core.offlineai

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.entity.AnalysisJobEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineModelRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: LoomoraDatabase
    private lateinit var repository: OfflineModelRepository
    private lateinit var alwaysCompatibleChecker: ModelCompatibilityChecker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LoomoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        alwaysCompatibleChecker = compatibleChecker()
        repository = OfflineModelRepository(
            offlineModelDao = database.offlineModelDao(),
            importer = OfflineModelImporter(
                context = context,
                json = Json { ignoreUnknownKeys = true },
                compatibilityChecker = alwaysCompatibleChecker
            ),
            compatibilityChecker = alwaysCompatibleChecker,
            catalog = DefaultOfflineModelCatalog(),
            json = Json { ignoreUnknownKeys = true }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importValid_persistsReadyModel() = runTest {
        val modelPack = createModelPack(
            manifest = manifest(id = "fixture-transcription", minimumRamMb = null),
            payloadBytes = "hello-model".toByteArray()
        )

        repository.importModel(Uri.fromFile(modelPack))

        val models = repository.models.first()
        val imported = models.first { it.manifest.id == "fixture-transcription" }
        assertEquals(ModelInstallState.READY, imported.state)
        assertNotNull(imported.installedPath)
    }

    @Test
    fun invalidChecksum_marksImportAsFailure() = runTest {
        val badManifest = manifest(id = "bad-checksum").copy(sha256 = "deadbeef")
        val modelPack = createModelPack(
            manifest = badManifest,
            payloadBytes = "wrong".toByteArray()
        )

        val result = runCatching {
            repository.importModel(Uri.fromFile(modelPack))
        }

        assertTrue(result.exceptionOrNull() is OfflineAiException.ModelChecksumMismatch)
    }

    @Test
    fun additionalFileChecksumMismatch_failsImport() = runTest {
        val encoderBytes = "encoder".toByteArray()
        val decoderBytes = "decoder".toByteArray()
        val badDecoder = OfflineModelFile(
            fileName = "decoder.onnx",
            sizeBytes = decoderBytes.size.toLong(),
            sha256 = "deadbeef"
        )
        val modelPack = createModelPack(
            manifest = manifest(id = "bad-decoder").copy(
                fileName = "encoder.onnx",
                sizeBytes = encoderBytes.size.toLong(),
                sha256 = sha256(encoderBytes),
                additionalFiles = listOf(badDecoder)
            ),
            payloadBytes = encoderBytes,
            payloadName = "encoder.onnx",
            additionalFiles = mapOf("decoder.onnx" to decoderBytes)
        )

        val result = runCatching {
            repository.importModel(Uri.fromFile(modelPack))
        }

        assertTrue(result.exceptionOrNull() is OfflineAiException.ModelChecksumMismatch)
    }

    @Test
    fun additionalFiles_arePublishedWithPrimaryPayload() = runTest {
        val encoderBytes = "encoder".toByteArray()
        val decoderBytes = "decoder".toByteArray()
        val modelPack = createModelPack(
            manifest = manifest(id = "multi-file-model").copy(
                fileName = "encoder.onnx",
                sizeBytes = encoderBytes.size.toLong(),
                sha256 = sha256(encoderBytes),
                additionalFiles = listOf(
                    OfflineModelFile(
                        fileName = "decoder.onnx",
                        sizeBytes = decoderBytes.size.toLong(),
                        sha256 = sha256(decoderBytes)
                    )
                )
            ),
            payloadBytes = encoderBytes,
            payloadName = "encoder.onnx",
            additionalFiles = mapOf("decoder.onnx" to decoderBytes)
        )

        repository.importModel(Uri.fromFile(modelPack))

        val model = repository.models.first().first { it.manifest.id == "multi-file-model" }
        val installedPayload = File(requireNotNull(model.installedPath))
        assertTrue(installedPayload.exists())
        assertTrue(File(installedPayload.parentFile, "decoder.onnx").exists())
    }

    @Test
    fun interruptedImport_doesNotPersistReadyModel() = runTest {
        val modelPack = createModelPack(
            manifest = manifest(id = "interrupted-model"),
            payloadBytes = ByteArray(64) { 1 }
        )
        val importer = object : OfflineModelImporter(
            context = context,
            json = Json { ignoreUnknownKeys = true },
            compatibilityChecker = alwaysCompatibleChecker
        ) {
            override suspend fun copyUriToFile(
                resolver: android.content.ContentResolver,
                sourceUri: Uri,
                destination: File
            ) {
                throw CancellationException("interrupted")
            }
        }

        val result = runCatching {
            importer.importModel(Uri.fromFile(modelPack))
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertTrue(database.offlineModelDao().getAllModels().isEmpty())
    }

    @Test
    fun duplicateImport_replacesExistingRow() = runTest {
        val modelPack = createModelPack(
            manifest = manifest(id = "duplicate-model", version = "1"),
            payloadBytes = "v1".toByteArray()
        )
        repository.importModel(Uri.fromFile(modelPack))

        val updatedPack = createModelPack(
            manifest = manifest(id = "duplicate-model", version = "2"),
            payloadBytes = "v2".toByteArray()
        )
        repository.importModel(Uri.fromFile(updatedPack))

        val model = repository.models.first().first { it.manifest.id == "duplicate-model" }
        assertEquals("2", model.manifest.version)
    }

    @Test
    fun incompatibleAbi_persistsIncompatibleState() = runTest {
        val incompatibleRepository = OfflineModelRepository(
            offlineModelDao = database.offlineModelDao(),
            importer = OfflineModelImporter(
                context = context,
                json = Json { ignoreUnknownKeys = true },
                compatibilityChecker = incompatibleChecker(
                    CompatibilityIssue.ABI_UNSUPPORTED,
                    "Unsupported ABI"
                )
            ),
            compatibilityChecker = incompatibleChecker(
                CompatibilityIssue.ABI_UNSUPPORTED,
                "Unsupported ABI"
            ),
            catalog = DefaultOfflineModelCatalog(),
            json = Json { ignoreUnknownKeys = true }
        )
        val modelPack = createModelPack(
            manifest = manifest(id = "abi-model", supportedAbis = setOf("riscv64")),
            payloadBytes = "abi".toByteArray()
        )

        incompatibleRepository.importModel(Uri.fromFile(modelPack))

        val model = incompatibleRepository.models.first().first { it.manifest.id == "abi-model" }
        assertEquals(ModelInstallState.INCOMPATIBLE, model.state)
    }

    @Test
    fun modelFileMissingAfterReady_surfacesCorruptState() = runTest {
        val modelPack = createModelPack(
            manifest = manifest(id = "missing-after-ready", minimumRamMb = null),
            payloadBytes = "bye".toByteArray()
        )
        repository.importModel(Uri.fromFile(modelPack))

        val entity = database.offlineModelDao().getModelById("missing-after-ready")!!
        File(requireNotNull(entity.installedPath)).delete()

        val model = repository.models.first().first { it.manifest.id == "missing-after-ready" }
        assertEquals(ModelInstallState.CORRUPT, model.state)
    }

    @Test
    fun removeModel_keepsExistingAnalysisJobs() = runTest {
        val modelPack = createModelPack(
            manifest = manifest(id = "remove-model"),
            payloadBytes = "keep-jobs".toByteArray()
        )
        repository.importModel(Uri.fromFile(modelPack))
        database.analysisJobDao().upsertJob(
            AnalysisJobEntity(
                id = "job-1",
                logicalKey = "rec|fp|v1|TRANSCRIPTION",
                recordingId = "rec-1",
                sourceFingerprint = "fp",
                pipelineVersion = "v1",
                requestedOptionsJson = "[]",
                status = AnalysisJobStatus.QUEUED.name,
                progress = 0f,
                attempt = 0,
                stageOutputRef = null,
                modelVersionsJson = "{}",
                errorCode = null,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        repository.removeModel("remove-model")

        assertEquals(1, database.analysisJobDao().observeJobsForRecording("rec-1").first().size)
    }

    private fun createModelPack(
        manifest: OfflineModelManifest,
        payloadBytes: ByteArray,
        payloadName: String = "model.bin",
        additionalFiles: Map<String, ByteArray> = emptyMap()
    ): File {
        val payloadSha = sha256(payloadBytes)
        val normalizedManifest = manifest.copy(
            fileName = payloadName,
            sizeBytes = payloadBytes.size.toLong(),
            sha256 = if (manifest.sha256 == "fixture") payloadSha else manifest.sha256
        )
        val file = temporaryFolder.newFile("${manifest.id}-${manifest.version}.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(Json.encodeToString(OfflineModelManifest.serializer(), normalizedManifest).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(payloadName))
            zip.write(payloadBytes)
            zip.closeEntry()

            additionalFiles.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun manifest(
        id: String,
        version: String = "1",
        minimumRamMb: Int? = null,
        supportedAbis: Set<String> = setOf("arm64-v8a", "x86_64"),
        sha256: String = "fixture"
    ): OfflineModelManifest {
        return OfflineModelManifest(
            id = id,
            version = version,
            capability = ModelCapability.TRANSCRIPTION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "model.bin",
            sizeBytes = 0,
            sha256 = sha256,
            minimumRamMb = minimumRamMb,
            supportedAbis = supportedAbis,
            supportedLanguages = setOf("en"),
            licenseName = "Apache-2.0",
            licenseUrl = "https://example.test/license",
            sourceUrl = "https://example.test/source",
            pipelineCompatibility = OfflineAiRuntimeVersions.PIPELINE_VERSION
        )
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private fun compatibleChecker(): ModelCompatibilityChecker {
        return object : ModelCompatibilityChecker(context) {
            override fun evaluate(manifest: OfflineModelManifest): CompatibilityResult {
                return CompatibilityResult.Compatible(ExecutionBackend.CPU)
            }
        }
    }

    private fun incompatibleChecker(
        issue: CompatibilityIssue,
        detail: String
    ): ModelCompatibilityChecker {
        return object : ModelCompatibilityChecker(context) {
            override fun evaluate(manifest: OfflineModelManifest): CompatibilityResult {
                return CompatibilityResult.Incompatible(issue, detail)
            }
        }
    }
}
