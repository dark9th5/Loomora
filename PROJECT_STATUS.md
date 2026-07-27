# Project Status

Date: 2026-07-27

## Current Milestone

P2 Offline AI foundation.

## Completed

- P0.1 CI baseline is green locally:
  - `check` passes.
  - `testDebugUnitTest` passes.
  - `assembleDebug` passes.
  - `assembleRelease` passes without debug signing.
- CodeGraph has been initialized for local code exploration.
- P0.2 production release signing path is configured:
  - Release no longer falls back to debug signing.
  - Local signing can use environment variables or untracked `keystore.properties`.
  - Manual GitHub release workflow can use repository secrets.
  - PR-safe `assembleRelease` still compiles unsigned when production secrets are absent.
- P0.3 durable recording session and markers:
  - Service creates and persists a real recording ID before recorder start.
  - Engine, service commands, output file, UI state, and marker foreign keys share that ID.
  - Marker count is backed by Room Flow for the active recording.
  - Recorder pause/resume/stop commands go through the service command path.
- P0.4 recorder UX/save acknowledgement code path:
  - Opening the recorder and granting microphone permission no longer starts recording.
  - Recording starts only from the explicit Record action, with duplicate start guarded.
  - Stop snapshots final duration before save/reset and excludes paused time.
  - Service emits `Saved` only after Room persistence succeeds; save failure stays typed and recoverable.
  - System Back now shows stop/save confirmation during active sessions.
  - Home/Library recording lists only show `SAVED` rows.
  - Physical smoke testing was run on `CPH2339` with debug APK.
- P0.5 resource cleanup and localization:
  - Recorder/service/player long-lived scopes use `SupervisorJob` where applicable.
  - `AudioRecordEngine.release()` cancels timer/amplitude sampling, releases `MediaRecorder`, and resets transient state idempotently.
  - `AudioRecorderService.onDestroy()` cancels service work and releases recorder resources.
  - `AudioPlayerEngine.release()` explicitly releases ExoPlayer; `RecordingDetailViewModel.onCleared()` calls it.
  - Recorder screen and recorder notification user-facing strings now resolve through English/Vietnamese resources.
  - Typed recorder errors are mapped to localized UI text at the UI boundary.
- P1.1 backend recovery scanner:
  - Startup recovery now scans interrupted `RECORDING`, `PAUSED`, and `FINALIZING` rows.
  - File validation uses Android media metadata plus audio-track probing before a partial file can become `SAVED`.
  - Missing, zero-byte, corrupt, recovered, and orphan files are represented through explicit `recoveryState` values.
  - Playable interrupted recordings are marked `SAVED` only after validation and keep the original file.
  - Orphan `.m4a` files create deterministic diagnostic rows without duplicate inserts on repeated scans.
  - Expired `.tmp` files in the recordings directory have a 7-day retention policy; original `.m4a` files are not silently deleted.
  - Recovery launches from `LoomoraApplication` in an IO `SupervisorJob` scope and preserves coroutine cancellation.
  - Library now surfaces `RECOVERY_FAILED` diagnostics with localized issue text and explicit `Open`, `Keep`, and confirmed `Delete` actions.
- P1.2 library operations and storage safety:
  - Added dedicated rename operation plus typed result handling for rename, trash, restore, permanent delete, export, and missing-source cases.
  - Added staged permanent-delete file handling through `RecordingFileSystem` instead of direct best-effort deletion.
  - Added `RecordingStorageManager` for storage category usage, free-space checks, share intents, and SAF export copy.
  - Library now supports trash mode, restore, permanent delete, and storage usage/low-space state.
  - Recording detail now exposes rename, share, export, trash, restore, and permanent delete flows backed by repository/storage services.
  - Recorder start now checks local free storage before issuing the start command.
  - App debug assembly and task-related unit suites pass after wiring `RecordingFileSystem` into Hilt.
- P1.3 persisted waveform pipeline:
  - Added streaming waveform extraction from saved audio files in `:core:audio` instead of reusing live recorder amplitudes.
  - Added SHA-256 source fingerprinting plus file-backed cache keyed by fingerprint, algorithm version, and resolution.
  - Recording detail and editor now load waveform state in the background and render loading/error/success UI.
  - Recording detail waveform now seeks through the existing millisecond playback timeline.
  - Added deterministic waveform tests for silence, tone, corrupt input, cache hit, cache invalidation, long-file fixed-bin output, and timestamp/bin mapping.
- P1.4 non-destructive audio editor export:
  - Replaced the fake copy-file exporter with a real export pipeline behind an `AudioEditEngine` boundary in `:core:audio`.
  - Added recipe validation/normalization, source fingerprint, recipe revision, and keep-range derivation in `:core:model`.
  - Export now writes to temp output, validates real output metadata, then publishes a new recording file without overwriting the original.
  - Editor now shows export progress, cancel state, preview segments/duration, and share flow for the exported result.
  - Added exporter/recipe unit coverage plus ADR and limitations docs for unsupported operations and Media3/device constraints.
- P2.1 offline AI foundation and model manager:
  - Added a real `:core:offlineai` boundary for offline model/install state, compatibility checks, local engine contracts, and analysis-job orchestration.
  - Removed production processing flow through the deleted `core:network` AI classes (`AiPipelineEngine`, `AiServiceContract`, `DefaultAiProvider`).
  - Added durable Room tables and migration coverage for `offline_models` and `analysis_jobs`.
  - Added SAF-based model import with streamed copy, SHA-256 verification, atomic publish, and persistent installed/corrupt/incompatible state.
  - Added `OfflineEngineLifecycleManager` so native runtime ownership is not held by ViewModels.
  - Rewired recording detail to queue local analysis through `OfflineAnalysisCoordinator`.
  - Added minimal settings UI for import/remove and installed/missing/verifying/corrupt/incompatible model states plus manifest license/source details.
  - Verified task-relevant tests plus `:app:assembleDebug` locally on Monday, July 27, 2026.
- P2.2 local offline transcription foundation:
  - Added durable transcript revision and segment schema in Room version `3`.
  - Added transaction-backed transcript publishing with stable revision/segment IDs, raw text, normalized text, model version, source fingerprint, and pipeline version.
  - Added streaming PCM16 WAV preprocessing to 16 kHz mono temp PCM plus simple speech-window detection and deterministic temp cleanup.
  - Extended transcription preprocessing for Android-supported compressed audio such as AAC/M4A through `MediaExtractor`/`MediaCodec`, streaming decoded PCM chunks to 16 kHz mono temp PCM without full-file PCM buffering.
  - Extended `OfflineAnalysisCoordinator` to run a local transcription flow through `LocalTranscriptionEngine`, update `analysis_jobs`, persist transcript output, and surface typed model/file/runtime/cancel states.
  - Added official upstream `sherpa-onnx-1.13.4.aar` to `:core:offlineai` and wired `SherpaOnnxTranscriptionEngine` to the Android offline Whisper recognizer API.
  - Model import now publishes the full verified model directory so Whisper encoder/decoder ONNX files can remain together after SAF import.
  - Compatibility checks now compare model RAM requirements against total device RAM rather than Android app heap class, avoiding a false `RAM_INSUFFICIENT` result for the tiny Whisper model on devices that can run the native runtime.
  - Model manifests can now list additional files, and importer verification checks size/SHA-256 for every listed file before atomic publish.
  - Generated a local importable multilingual Whisper tiny int8 model pack under `build/model-fixtures` from Hugging Face mirror files.
  - Updated the default offline model catalog to advertise the real multilingual Whisper tiny int8 model manifest instead of the earlier fixture placeholder.
  - `SherpaOnnxTranscriptionEngine` now requires and passes the tokens file to the sherpa `OfflineModelConfig`.
  - Added recording-detail transcript rendering and click-to-seek behavior for persisted transcript segments.
  - Added focused unit coverage for model missing, Vietnamese fixture persistence, mixed-language text preservation, corrupt input, cancellation cleanup, retry/idempotency, multi-file model publish, secondary-file checksum failure, and missing-tokens typed failure.
  - Added focused unit coverage for silence handling and source-changed stale transcript invalidation.
  - Verified `:core:database:testDebugUnitTest`, `:core:offlineai:testDebugUnitTest`, `:feature:recordingdetail:testDebugUnitTest`, and `:app:assembleDebug` locally.
  - Verified real sherpa-onnx Whisper tiny int8 transcription on a physical OPPO `CPH2339` over Wi-Fi ADB with direct instrumentation: `OK (1 test)`.
  - Verified generated AAC/M4A decode/resample preprocessing on the physical OPPO `CPH2339` with direct instrumentation: `OK (2 tests)` for the combined sherpa fixture and AAC/M4A preprocessing smoke.
  - Verified airplane-mode execution over USB ADB on the physical OPPO `CPH2339`: airplane mode reported `enabled`, the combined sherpa fixture and AAC/M4A preprocessing instrumentation returned `OK (2 tests)`, and airplane mode was restored to `disabled`.
  - Verified visible Settings UI/SAF import on the physical OPPO `CPH2339`: selecting the generated multilingual model pack from Downloads changed the transcription model state to `READY`.
  - Fixed imported-model native loading by using a null sherpa `AssetManager` for absolute app-storage model paths.
  - Settings model import now prefers zip/model-pack MIME types in the SAF picker.

## Next

- Follow-up hardening: run physical-device verification for larger-model storage handling, Vietnamese/mixed-language ASR quality, and longer recorder-produced AAC/M4A files.
- Follow-up hardening: run physical-device verification for large-model storage handling, Vietnamese/mixed-language ASR quality, recorder-produced AAC/M4A decode, and airplane-mode behavior.
- Release hardening later: enable and verify R8 after broader regression coverage.
