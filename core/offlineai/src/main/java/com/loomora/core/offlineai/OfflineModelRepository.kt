package com.loomora.core.offlineai

import android.net.Uri
import com.loomora.core.database.entity.OfflineModelEntity
import com.loomora.core.database.dao.OfflineModelDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineModelRepository @Inject constructor(
    private val offlineModelDao: OfflineModelDao,
    private val importer: OfflineModelImporter,
    private val compatibilityChecker: ModelCompatibilityChecker,
    private val catalog: DefaultOfflineModelCatalog,
    private val json: Json
) {
    val models: Flow<List<OfflineModelRecord>> = offlineModelDao.observeModels().map { entities ->
        val entityById = entities.associateBy { it.modelId }
        val catalogRecords = catalog.manifests.map { manifest ->
            val entity = entityById[manifest.id]
            if (entity == null) {
                OfflineModelRecord(
                    manifest = manifest,
                    state = ModelInstallState.NOT_INSTALLED,
                    installedPath = null,
                    installedAt = null,
                    lastVerifiedAt = null,
                    compatibility = compatibilityChecker.evaluate(manifest),
                    errorCode = null
                )
            } else {
                entity.toRecord(json, compatibilityChecker.evaluate(manifest)).withValidatedState()
            }
        }
        val extraRecords = entities
            .filter { entity -> catalog.manifests.none { it.id == entity.modelId } }
            .map { entity ->
                val manifest = OfflineModelManifest(
                    id = entity.modelId,
                    version = entity.version,
                    capability = ModelCapability.valueOf(entity.capability),
                    runtime = RuntimeKind.valueOf(entity.runtime),
                    fileName = entity.fileName,
                    sizeBytes = entity.sizeBytes,
                    sha256 = entity.sha256,
                    minimumRamMb = entity.minimumRamMb,
                    supportedAbis = json.decodeFromString(SetSerializer(String.serializer()), entity.supportedAbisJson),
                    supportedLanguages = json.decodeFromString(SetSerializer(String.serializer()), entity.supportedLanguagesJson),
                    licenseName = entity.licenseName,
                    licenseUrl = entity.licenseUrl,
                    sourceUrl = entity.sourceUrl,
                    pipelineCompatibility = entity.pipelineCompatibility
                )
                entity.toRecord(json, compatibilityChecker.evaluate(manifest)).withValidatedState()
            }
        (catalogRecords + extraRecords).sortedWith(compareBy({ it.manifest.capability.name }, { it.manifest.id }))
    }

    suspend fun importModel(sourceUri: Uri) {
        val imported = importer.importModel(sourceUri) { manifest, state ->
            offlineModelDao.upsertModel(
                manifest.toEntity(
                    json = json,
                    state = state,
                    installedPath = null,
                    installedAt = null,
                    lastVerifiedAt = null,
                    errorCode = null
                )
            )
        }
        val now = System.currentTimeMillis()
        val state = when (imported.compatibility) {
            is CompatibilityResult.Compatible -> ModelInstallState.READY
            is CompatibilityResult.Incompatible -> ModelInstallState.INCOMPATIBLE
        }
        offlineModelDao.upsertModel(
            imported.manifest.toEntity(
                json = json,
                state = state,
                installedPath = imported.publishedPayload?.absolutePath,
                installedAt = if (state == ModelInstallState.READY) now else null,
                lastVerifiedAt = now,
                errorCode = when (imported.compatibility) {
                    is CompatibilityResult.Compatible -> null
                    is CompatibilityResult.Incompatible -> imported.compatibility.issue.name
                }
            )
        )
    }

    suspend fun removeModel(modelId: String) {
        val existing = offlineModelDao.getModelById(modelId) ?: return
        existing.installedPath?.let { path ->
            File(path).parentFile?.deleteRecursively()
        }
        offlineModelDao.deleteModel(modelId)
    }

    suspend fun hasReadyModels(requiredCapabilities: Set<ModelCapability>): Boolean {
        val installed = offlineModelDao.getAllModels()
        return requiredCapabilities.all { capability ->
            installed.any { it.capability == capability.name && it.state == ModelInstallState.READY.name }
        }
    }

    suspend fun getReadyModel(capability: ModelCapability): OfflineModelRecord? {
        return offlineModelDao.getAllModels()
            .firstOrNull { it.capability == capability.name && it.state == ModelInstallState.READY.name }
            ?.let { entity ->
                val manifest = OfflineModelManifest(
                    id = entity.modelId,
                    version = entity.version,
                    capability = ModelCapability.valueOf(entity.capability),
                    runtime = RuntimeKind.valueOf(entity.runtime),
                    fileName = entity.fileName,
                    sizeBytes = entity.sizeBytes,
                    sha256 = entity.sha256,
                    minimumRamMb = entity.minimumRamMb,
                    supportedAbis = json.decodeFromString(SetSerializer(String.serializer()), entity.supportedAbisJson),
                    supportedLanguages = json.decodeFromString(SetSerializer(String.serializer()), entity.supportedLanguagesJson),
                    licenseName = entity.licenseName,
                    licenseUrl = entity.licenseUrl,
                    sourceUrl = entity.sourceUrl,
                    pipelineCompatibility = entity.pipelineCompatibility
                )
                entity.toRecord(json, compatibilityChecker.evaluate(manifest)).withValidatedState()
                    .takeIf { it.state == ModelInstallState.READY }
            }
    }

    suspend fun getModelDetails(modelId: String): OfflineModelRecord? {
        val manifest = catalog.manifests.firstOrNull { it.id == modelId } ?: return null
        val entity = offlineModelDao.getModelById(modelId)
        return if (entity == null) {
            OfflineModelRecord(
                manifest = manifest,
                state = ModelInstallState.NOT_INSTALLED,
                installedPath = null,
                installedAt = null,
                lastVerifiedAt = null,
                compatibility = compatibilityChecker.evaluate(manifest),
                errorCode = null
            )
        } else {
            entity.toRecord(json, compatibilityChecker.evaluate(manifest)).withValidatedState()
        }
    }

    private fun OfflineModelEntity.toRecord(
        json: Json,
        compatibility: CompatibilityResult
    ): OfflineModelRecord {
        return OfflineModelRecord(
            manifest = OfflineModelManifest(
                id = modelId,
                version = version,
                capability = ModelCapability.valueOf(capability),
                runtime = RuntimeKind.valueOf(runtime),
                fileName = fileName,
                sizeBytes = sizeBytes,
                sha256 = sha256,
                minimumRamMb = minimumRamMb,
                supportedAbis = json.decodeFromString(SetSerializer(String.serializer()), supportedAbisJson),
                supportedLanguages = json.decodeFromString(SetSerializer(String.serializer()), supportedLanguagesJson),
                licenseName = licenseName,
                licenseUrl = licenseUrl,
                sourceUrl = sourceUrl,
                pipelineCompatibility = pipelineCompatibility
            ),
            state = ModelInstallState.valueOf(state),
            installedPath = installedPath,
            installedAt = installedAt,
            lastVerifiedAt = lastVerifiedAt,
            compatibility = compatibility,
            errorCode = errorCode
        )
    }

    private fun OfflineModelRecord.withValidatedState(): OfflineModelRecord {
        if (state == ModelInstallState.READY && installedPath != null && !File(installedPath).exists()) {
            return copy(
                state = ModelInstallState.CORRUPT,
                errorCode = OfflineAiException.ModelFileMissing::class.simpleName
            )
        }
        return this
    }

    private fun OfflineModelManifest.toEntity(
        json: Json,
        state: ModelInstallState,
        installedPath: String?,
        installedAt: Long?,
        lastVerifiedAt: Long?,
        errorCode: String?
    ): OfflineModelEntity {
        return OfflineModelEntity(
            modelId = id,
            version = version,
            capability = capability.name,
            runtime = runtime.name,
            fileName = fileName,
            sizeBytes = sizeBytes,
            sha256 = sha256,
            minimumRamMb = minimumRamMb,
            supportedAbisJson = json.encodeToString(SetSerializer(String.serializer()), supportedAbis),
            supportedLanguagesJson = json.encodeToString(SetSerializer(String.serializer()), supportedLanguages),
            licenseName = licenseName,
            licenseUrl = licenseUrl,
            sourceUrl = sourceUrl,
            pipelineCompatibility = pipelineCompatibility,
            state = state.name,
            installedPath = installedPath,
            installedAt = installedAt,
            lastVerifiedAt = lastVerifiedAt,
            errorCode = errorCode
        )
    }
}
