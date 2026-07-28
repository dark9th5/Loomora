# Model Manifest Report

Date: 2026-07-28

Loomora does not bundle large speech/LLM models in the base APK. Models are imported as verified model packs and published atomically after checksum validation.

| Capability | Model direction | Status | Approx size/evidence | License/source |
| --- | --- | --- | --- | --- |
| Offline transcription | sherpa-onnx Whisper tiny multilingual int8 | Beta/available with installed model | Local generated model pack `build/model-fixtures/sherpa-onnx-whisper-tiny-int8-multilingual.modelpack.zip`, 60,745,915 bytes. Device smoke passed on OPPO `CPH2339`. | sherpa-onnx `1.13.4`; model files from Hugging Face mirror as recorded in P2.2 notes. |
| Speaker diarization | sherpa-onnx pyannote segmentation + 3D-Speaker INT8 | Beta | Local generated model pack `build/diarization-fixtures/sherpa-onnx-pyannote-3-0-3dspeaker-int8.modelpack.zip`, 26,984,551 bytes. Device smoke passed on OPPO `CPH2339`. | sherpa-onnx release assets; Apache/source attribution required. |
| Lightweight insights | deterministic heuristic/extractive engine | Available/Beta | No model file; model size recorded as `0` bytes; device smoke passed. | App code. Evidence-linked, not generative LLM. |
| Deep generative insights | LiteRT-LM / llama.cpp / other future runtime | Coming Soon / Experimental | Granite 350M and Gemma/mobile-action attempts either failed strict JSON or hit memory/OEM kill on reference device. | Not release-available. Do not advertise as Available. |

## Distribution Rules

- Do not bundle all large models into base APK without a documented reason.
- Verify size and SHA-256 for every model file listed in the manifest before publish.
- Remove model must not delete historical transcript, diarization, or insight revisions.
- Temp PCM/model processing must preflight storage before heavy work where possible.
