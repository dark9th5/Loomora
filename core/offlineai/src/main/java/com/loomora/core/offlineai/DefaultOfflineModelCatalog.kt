package com.loomora.core.offlineai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOfflineModelCatalog @Inject constructor() {
    val manifests: List<OfflineModelManifest> = listOf(
        OfflineModelManifest(
            id = "sherpa-onnx-whisper-tiny-int8-multilingual",
            version = "asr-models-2026-07-27",
            capability = ModelCapability.TRANSCRIPTION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "tiny-encoder.int8.onnx",
            sizeBytes = 12_937_772,
            sha256 = "d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434",
            minimumRamMb = 2048,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("vi", "en", "multilingual"),
            licenseName = "Apache-2.0",
            licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
            sourceUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny",
            pipelineCompatibility = OfflineAiRuntimeVersions.PIPELINE_VERSION,
            additionalFiles = listOf(
                OfflineModelFile(
                    fileName = "tiny-decoder.int8.onnx",
                    sizeBytes = 89_855_401,
                    sha256 = "d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925"
                ),
                OfflineModelFile(
                    fileName = "tiny-tokens.txt",
                    sizeBytes = 816_730,
                    sha256 = "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126"
                )
            )
        ),
        OfflineModelManifest(
            id = "meeting-insights-fixture",
            version = "1",
            capability = ModelCapability.INSIGHTS,
            runtime = RuntimeKind.LITERT_LM,
            fileName = "model.litertlm",
            sizeBytes = 1,
            sha256 = "fixture",
            minimumRamMb = 512,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("en", "vi"),
            licenseName = "Fixture",
            licenseUrl = null,
            sourceUrl = null,
            pipelineCompatibility = OfflineAiRuntimeVersions.PIPELINE_VERSION
        )
    )
}
