package com.loomora.core.offlineai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOfflineModelCatalog @Inject constructor() {
    val manifests: List<OfflineModelManifest> = listOf(
        OfflineModelManifest(
            id = "sherpa-onnx-zipformer-vi-30m-int8-2026-02-09",
            version = "2026-02-09",
            capability = ModelCapability.TRANSCRIPTION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "encoder.int8.onnx",
            sizeBytes = 27_699_063,
            sha256 = "8ef5286dd427eb108055c2ddc1982aa31e544706072d5ea228729292dacade68",
            minimumRamMb = 2048,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("vi"),
            licenseName = "Apache-2.0",
            licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
            sourceUrl = "https://huggingface.co/csukuangfj2/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09",
            pipelineCompatibility = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
            additionalFiles = listOf(
                OfflineModelFile("decoder.onnx", 5_165_084, "cf2aa385b82c9d5d40cd29c3188af52d0249b3b78f0d4b7eb84ad502d50c7e7f"),
                OfflineModelFile("joiner.int8.onnx", 1_033_417, "7311d2e17b810ecea515d79c71cc4668af8759256a06fa01d27047772320c821"),
                OfflineModelFile("tokens.txt", 23_238, "ca8171f8bbd516c050b627582f2125c8f5f1f6ed967ab41b0fa9aae2cf61b492")
            )
        ),
        OfflineModelManifest(
            id = "sherpa-onnx-silero-vad-16k",
            version = "asr-models-2026-07-29",
            capability = ModelCapability.VOICE_ACTIVITY_DETECTION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "silero_vad.onnx",
            sizeBytes = 643_854,
            sha256 = "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6",
            minimumRamMb = 1024,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("multilingual"),
            licenseName = "MIT",
            licenseUrl = "https://github.com/snakers4/silero-vad/blob/master/LICENSE",
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models",
            pipelineCompatibility = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION
        ),
        OfflineModelManifest(
            id = "sherpa-onnx-whisper-base-int8-multilingual",
            version = "asr-models-2026-07-29",
            capability = ModelCapability.TRANSCRIPTION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "base-encoder.int8.onnx",
            sizeBytes = 29_120_534,
            sha256 = "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11",
            minimumRamMb = 3072,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("vi", "en", "multilingual"),
            licenseName = "Apache-2.0",
            licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
            sourceUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base",
            pipelineCompatibility = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
            additionalFiles = listOf(
                OfflineModelFile(
                    fileName = "base-decoder.int8.onnx",
                    sizeBytes = 130_672_026,
                    sha256 = "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d"
                ),
                OfflineModelFile(
                    fileName = "base-tokens.txt",
                    sizeBytes = 816_730,
                    sha256 = "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126"
                )
            )
        ),
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
            version = "litertlm-reference-2026-07-28",
            capability = ModelCapability.INSIGHTS,
            runtime = RuntimeKind.LITERT_LM,
            fileName = "model.litertlm",
            sizeBytes = -1,
            sha256 = "model-pack-manifest-required",
            minimumRamMb = 3072,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("en", "vi"),
            licenseName = "Model-specific",
            licenseUrl = null,
            sourceUrl = "https://developers.google.com/edge/litert-lm/android",
            pipelineCompatibility = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION
        ),
        OfflineModelManifest(
            id = "sherpa-onnx-pyannote-3-0-3dspeaker-int8",
            version = "diarization-models-2026-07-29",
            capability = ModelCapability.DIARIZATION,
            runtime = RuntimeKind.SHERPA_ONNX,
            fileName = "segmentation-pyannote-3.0.onnx",
            sizeBytes = 1_540_506,
            sha256 = "d582f4b4c6b48205de7e0643c57df0df5615a3c176189be3fc461e9d18827b5d",
            minimumRamMb = 2048,
            supportedAbis = setOf("arm64-v8a", "x86_64"),
            supportedLanguages = setOf("vi", "en", "multilingual"),
            licenseName = "MIT + Apache-2.0",
            licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
            sourceUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0",
            pipelineCompatibility = OfflineAiRuntimeVersions.DIARIZATION_PIPELINE_VERSION,
            additionalFiles = listOf(
                OfflineModelFile(
                    fileName = "3dspeaker_speech_campplus_sv_zh_en_16k-common_advanced.onnx",
                    sizeBytes = 28_281_164,
                    sha256 = "aa3cfc16963a10586a9393f5035d6d6b57e98d358b347f80c2a30bf4f00ceba2"
                )
            )
        )
    )

    companion object {
        const val RECOMMENDED_TRANSCRIPTION_MODEL_ID = "sherpa-onnx-zipformer-vi-30m-int8-2026-02-09"
        const val ACCURATE_TRANSCRIPTION_MODEL_ID = "sherpa-onnx-whisper-base-int8-multilingual"
        const val RECOMMENDED_VAD_MODEL_ID = "sherpa-onnx-silero-vad-16k"
        const val RECOMMENDED_DIARIZATION_MODEL_ID = "sherpa-onnx-pyannote-3-0-3dspeaker-int8"
    }
}
