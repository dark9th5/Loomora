package com.loomora.core.offlineai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptionModelSelectorTest {
    private fun record(id: String, languages: Set<String>) = OfflineModelRecord(
        manifest = OfflineModelManifest(
            id = id,
            version = "1",
            capability = ModelCapability.TRANSCRIPTION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "$id.onnx",
            sizeBytes = 1L,
            sha256 = "0".repeat(64),
            supportedAbis = setOf("arm64-v8a"),
            supportedLanguages = languages,
            licenseName = "test",
            pipelineCompatibility = "test"
        ),
        state = ModelInstallState.READY,
        installedPath = "/tmp/$id.onnx",
        installedAt = 1L,
        lastVerifiedAt = 1L,
        compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
    )

    private val vi = record("vi-specialized", setOf("vi"))
    private val multilingual = record("whisper-multilingual", setOf("vi", "en", "multilingual"))

    @Test
    fun vietnamesePrefersSpecializedModel() {
        assertEquals("vi-specialized", TranscriptionModelSelector.select("vi", listOf(multilingual, vi))?.manifest?.id)
    }

    @Test
    fun englishNeverUsesVietnameseOnlyModel() {
        assertEquals("whisper-multilingual", TranscriptionModelSelector.select("en", listOf(vi, multilingual))?.manifest?.id)
    }

    @Test
    fun incompatibleManualChoiceIsRejected() {
        assertNull(TranscriptionModelSelector.select("en", listOf(vi, multilingual), "vi-specialized"))
    }

    @Test
    fun autoPrefersMultilingual() {
        assertEquals("whisper-multilingual", TranscriptionModelSelector.select("auto", listOf(vi, multilingual))?.manifest?.id)
    }
}
