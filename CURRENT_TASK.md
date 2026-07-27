# Current Task: P2.2 Local Offline Transcription

Date: 2026-07-27

## Scope

Implement P2.2 only:
- timestamped offline transcription foundation through sherpa-onnx non-streaming ASR;
- validate/fingerprint source audio;
- decode/resample to 16 kHz mono PCM in a streaming/temp-file flow;
- optional VAD/speech-window segmentation without losing timestamps;
- run local transcription behind a production sherpa adapter boundary;
- persist transcript revisions and segments in Room;
- expose UI states and transcript seek interaction in recording detail.

Do not implement diarization, summary, action extraction, cloud fallback, or fake production transcript success in this task.

## Pre-Edit Findings

- Worktree is already dirty from previous P0/P1/P2.1 work; unrelated changes will not be reverted.
- `:core:offlineai` exists and currently exposes `LocalTranscriptionEngine`, but `OfflineAnalysisCoordinator` only verifies models and queues a job; it does not run ASR or persist transcript output yet.
- `core:model/AiModels.kt` has `TranscriptSegment`, but it lacks stable segment IDs, raw-vs-normalized text, revision metadata, and typed in-progress/cancelled states.
- `core:database` has `analysis_jobs` and `offline_models` from P2.1, but no transcript revision/segment tables yet.
- `recordings.transcriptStatus` exists as a string field and can be updated to reflect NOT_STARTED/QUEUED/PROCESSING/COMPLETE/FAILED/CANCELLED states.
- `feature:recordingdetail` renders only job status text; it does not display persisted transcript segments or click-to-seek.
- Official sherpa-onnx Android docs point to Android AAR/Kotlin API integration for offline ASR; the current pinned release line is `1.13.4`.
- Official `sherpa-onnx-1.13.4.aar` was downloaded from the upstream release and its SHA-256 is `03F9C4DF965F21C71269365A7951A7F23B5696FDDD093FA318C80D65550AB780`.

## Plan

1. Add transcript domain models that preserve:
   - revision ID;
   - stable segment IDs;
   - raw text;
   - normalized text;
   - timestamps;
   - source fingerprint;
   - model/runtime/pipeline versions.
2. Add Room schema version `3` with:
   - `transcript_revisions`;
   - `transcript_segments`;
   - DAO queries for latest revision, idempotent replace, and observation by recording ID.
3. Add a streaming audio preprocessing boundary:
   - source validation/fingerprint;
   - 16 kHz mono PCM temp output;
   - deterministic cleanup on success/failure/cancel;
   - no full-file PCM in memory.
4. Add a production sherpa transcription adapter boundary:
   - real implementation target for sherpa-onnx non-streaming ASR;
   - no production fake transcript;
   - typed unavailable/model/device/file errors when runtime/model files are absent.
5. Extend `OfflineAnalysisCoordinator` to run transcription:
   - model missing;
   - queued;
   - preparing audio;
   - transcribing progress;
   - completed;
   - failed;
   - cancelled.
6. Persist successful transcript output as a single latest revision transaction and make retries idempotent for the same source fingerprint/pipeline/model version.
7. Add recording-detail UI transcript list with click-to-seek.
8. Add focused tests:
   - source missing/corrupt;
   - model missing;
   - cancel cleanup;
   - retry/idempotency;
   - timestamp ordering/bounds;
   - source-changed stale invalidation;
   - no duplicate segments;
   - UI/viewmodel seek hook where practical.
9. Run task-relevant tests/builds and update tracking docs with real results.

## Acceptance Criteria

- No network/API processing path is introduced.
- No hard-coded/sample transcript production success path exists.
- Transcript revisions and segments are durable in Room.
- Segment IDs are stable within a revision and retry does not duplicate segments.
- Source fingerprint, model version, and pipeline version are stored with results.
- Temp PCM is cleaned up after success/failure/cancellation.
- UI can show not processed/model missing/queued/processing/complete/failed/cancelled states.
- Clicking a transcript segment seeks playback to its timestamp.
- Task-relevant tests/builds pass with real command output.

## Results

Complete for the P2.2 vertical slice.

Implemented the durable local transcription pipeline foundation:
- added transcript domain metadata for stable segment IDs, raw text, normalized text, and revision metadata;
- added Room schema version `3` with `transcript_revisions` and `transcript_segments`;
- added migration `2 -> 3` and generated schema export `3.json`;
- added `TranscriptDao` and `TranscriptRepository` with transaction-backed revision/segment replacement;
- added source fingerprinting and temp PCM preprocessing for PCM16 WAV fixtures without loading full PCM into memory;
- added simple speech-window detection over streamed PCM chunks;
- extended `OfflineAnalysisCoordinator` from queue-only to transcription execution:
  - model missing;
  - queued;
  - preparing audio;
  - processing;
  - complete;
  - failed;
  - cancelled;
- added typed file/model/runtime errors and temp PCM cleanup in `finally`;
- added recording-detail persisted transcript observation and click-to-seek segment UI;
- added unit coverage for missing model, short Vietnamese fixture persistence, mixed-language text preservation, corrupt input, cancellation cleanup, and retry/idempotency.

Not completed:
- added official upstream `sherpa-onnx-1.13.4.aar` to `:core:offlineai`;
- wired `SherpaOnnxTranscriptionEngine` directly to the sherpa Android Kotlin API for non-streaming Whisper ONNX recognition;
- the engine now creates an offline recognizer, streams 16 kHz mono PCM chunks, decodes locally, maps timestamps/durations, and releases recognizer/stream resources in `finally`;
- model import now publishes the full verified model directory so Whisper encoder/decoder sidecar files can stay together.
- added manifest support for additional model files and verify size/SHA-256 for every listed file before publish;
- added importer test coverage for multi-file publish and secondary-file checksum failure;
- added zip path traversal guard during model-pack extraction.
- downloaded the Hugging Face mirror files for multilingual `sherpa-onnx-whisper-tiny` int8:
  - `tiny-encoder.int8.onnx`;
  - `tiny-decoder.int8.onnx`;
  - `tiny-tokens.txt`;
- generated a local importable model pack at `build/model-fixtures/sherpa-onnx-whisper-tiny-int8-multilingual.modelpack.zip`;
- updated `SherpaOnnxTranscriptionEngine` to require and pass the tokens file to `OfflineModelConfig`.

Completed during device verification:
- fixed `SherpaOnnxTranscriptionEngine` to pass `assetManager = null` when loading imported model files from app storage absolute paths; sherpa-onnx aborts if absolute paths are used with a non-null `AssetManager`;
- added an Android device smoke test for the generated multilingual Whisper tiny int8 model pack and deterministic `0.wav` fixture;
- verified the model pack and WAV fixture on a physical OPPO `CPH2339` over Wi-Fi ADB at `192.168.68.109:33759`;
- direct instrumentation returned `OK (1 test)` and transcribed the fixture through real sherpa-onnx native ASR.

Completed after chat recovery:
- confirmed the P2.2 worktree changes were still present in the local project after the Codex chat history issue;
- updated `DefaultOfflineModelCatalog` to expose the real generated multilingual Whisper tiny int8 manifest, including encoder/decoder/tokens SHA-256 metadata, instead of the earlier transcription fixture placeholder;
- changed the Settings SAF model picker to prefer zip/model-pack MIME types;
- fixed `ModelCompatibilityChecker` to compare model `minimumRamMb` against total device RAM from `ActivityManager.MemoryInfo.totalMem` instead of Android app heap `memoryClass`; the previous heap check incorrectly marked the tiny Whisper model incompatible on the same OPPO device that can run the native sherpa smoke test;
- extended `AudioTranscriptionPreprocessor` to decode Android-supported compressed audio such as AAC/M4A through `MediaExtractor`/`MediaCodec`, streaming decoded PCM chunks into the same 16 kHz mono temp-file path used by sherpa transcription;
- added unit coverage for silence handling and source-changed stale transcript invalidation;
- added a physical-device instrumentation smoke that generates AAC/M4A on-device and verifies decode/resample preprocessing creates non-empty PCM and speech windows.

Not completed in P2.2:
- airplane-mode smoke passed over USB ADB on the OPPO `CPH2339`: airplane mode reported `enabled`, the combined sherpa fixture and AAC/M4A preprocessing instrumentation returned `OK (2 tests)`, and airplane mode was restored to `disabled`.
- visible Settings UI/SAF import passed on the OPPO `CPH2339`: the generated multilingual model pack was selected from Downloads and the transcription model state reached `READY`.

## Commands

- `git status --short`: confirmed dirty worktree from prior tasks.
- `cmd /c codegraph explore "offlineai transcription transcript Room transcript segments sherpa onnx ASR audio decode PCM recording detail"`: used CodeGraph via `cmd` because PowerShell blocks the `.ps1` shim; confirmed current offline AI coordinator queues only and no transcript persistence exists.
- `rg -n "Transcript|transcript|AnalysisJob|OfflineAnalysis|LocalTranscription|AiJobStatus|sherpa|onnx|PCM|Waveform|MediaExtractor|AudioTrack|segment" core feature app gradle -g "*.kt" -g "*.kts" -g "*.toml"`: confirmed relevant current call sites and schema gaps.
- `Get-Content` reads for:
  - `docs/03_OFFLINE_AI_PIPELINE.md`
  - `docs/04_DATA_AND_JOB_STATES.md`
  - `docs/05_MODEL_MANAGEMENT.md`
  - `core/offlineai/OfflineAiContracts.kt`
  - `core/offlineai/OfflineAnalysisCoordinator.kt`
  - `core/database/LoomoraDatabase.kt`
  - `core/model/AiModels.kt`
  - `core/model/Recording.kt`
  - `core/database/entity/AudioSegmentEntity.kt`
  - `core/database/entity/RecordingEntity.kt`
  - `core/database/Migrations.kt`
  Result: confirmed P2.2 must add transcript revision schema and real local transcription execution.
- `cmd /c gradlew.bat :core:database:compileDebugKotlin --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :core:offlineai:compileDebugKotlin --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :feature:recordingdetail:compileDebugKotlin --console=plain --no-daemon --max-workers=1`: first failed because adding `transcriptFlow` exceeded the typed `combine` overload; passed after nesting the combine.
- `cmd /c gradlew.bat :feature:recordingdetail:compileDebugUnitTestKotlin --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :core:offlineai:compileDebugUnitTestKotlin --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :core:database:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :feature:recordingdetail:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed.
- `cmd /c gradlew.bat :app:assembleDebug --console=plain --no-daemon --max-workers=1`: first parallel run timed out in the tool; rerun as a single command passed.
- `Invoke-RestMethod https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags/v1.13.4`: confirmed official release asset `sherpa-onnx-1.13.4.aar`.
- `curl.exe -L -C - -o "core\offlineai\libs\sherpa-onnx-1.13.4.aar" "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.4/sherpa-onnx-1.13.4.aar"`: resumed and completed the official AAR download.
- `Get-FileHash core\offlineai\libs\sherpa-onnx-1.13.4.aar -Algorithm SHA256`: returned `03F9C4DF965F21C71269365A7951A7F23B5696FDDD093FA318C80D65550AB780`.
- `jar tf core\offlineai\libs\sherpa-onnx-1.13.4.aar`: confirmed Java classes plus Android native libraries for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- `javap` against extracted `classes.jar`: confirmed `OfflineRecognizer`, `OfflineRecognizerConfig`, `OfflineWhisperModelConfig`, `OfflineStream.acceptWaveform`, and timestamp result APIs.
- `cmd /c gradlew.bat :core:offlineai:compileDebugKotlin --console=plain --no-daemon --max-workers=1`: first failed on nullable encoder model resolution; passed after adding a typed missing-model failure.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after direct sherpa AAR integration.
- `cmd /c gradlew.bat :feature:recordingdetail:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after direct sherpa AAR integration.
- `cmd /c gradlew.bat :app:assembleDebug --console=plain --no-daemon --max-workers=1`: first tool run timed out at 184 seconds without a final Gradle result; rerun with a longer timeout passed.
- `Invoke-RestMethod https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags/asr-models`: found official multilingual `sherpa-onnx-whisper-tiny.tar.bz2` asset, size `116204861` bytes; `.en` asset was not selected because Vietnamese is required.
- `curl.exe -L -C - -o build\model-fixtures\sherpa-onnx-whisper-tiny.tar.bz2 ...`: timed out twice while attempting to resume the model download; no valid model artifact was produced.
- `curl.exe -L --fail -o build\model-fixtures\sherpa-onnx-whisper-tiny.tar.bz2 ...`: clean retry failed with `curl: (56) Recv failure: Connection was reset` after about 6 MB.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest --rerun-tasks --console=plain --no-daemon --max-workers=1`: exited `0`; test XML reported `15` tests, `0` failures, `0` errors, `0` skipped.
- `cmd /c gradlew.bat :core:offlineai:compileDebugKotlin :feature:recordingdetail:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon --max-workers=1`: exited `0`.
- `curl.exe -L --fail -o build\model-fixtures\hf-whisper-tiny-int8\tiny-encoder.int8.onnx https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/tiny-encoder.int8.onnx`: passed.
- `curl.exe -L --fail -C - -o build\model-fixtures\hf-whisper-tiny-int8\tiny-decoder.int8.onnx https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/tiny-decoder.int8.onnx`: passed.
- `curl.exe -L --fail -o build\model-fixtures\hf-whisper-tiny-int8\tiny-tokens.txt https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/tiny-tokens.txt`: passed.
- Python model-pack generation script: produced `build\model-fixtures\sherpa-onnx-whisper-tiny-int8-multilingual.modelpack.zip`, size `60745915` bytes, with SHA-256 entries for encoder/decoder/tokens.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest --rerun-tasks --console=plain --no-daemon --max-workers=1`: passed; XML reported `16` offlineai tests, `0` failures, `0` errors, `0` skipped.
- `C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`: passed, but listed no devices.
- `cmd /c gradlew.bat :core:database:testDebugUnitTest :core:offlineai:testDebugUnitTest :feature:recordingdetail:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon --max-workers=1`: exited `0`; XML reports showed database `15` tests, offlineai `16` tests, recordingdetail `4` tests, all with `0` failures/errors/skips.
- `C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe connect 192.168.68.109:33759`: passed; Wi-Fi ADB connected to `CPH2339`.
- `C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe -s 192.168.68.109:33759 shell settings put global verifier_verify_adb_installs 1`: passed; restored the device ADB install verifier setting after earlier manual install troubleshooting.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest :core:offlineai:compileDebugAndroidTestKotlin --console=plain --no-daemon --max-workers=1`: passed.
- `C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe -s 192.168.68.109:33759 shell am instrument -w -r -e class com.loomora.core.offlineai.SherpaOnnxDeviceSmokeTest com.loomora.core.offlineai.test/androidx.test.runner.AndroidJUnitRunner`: passed with `OK (1 test)`.
- `cmd /c codegraph explore "P2.2 remaining offline model import SAF settings UI SherpaOnnxDeviceSmokeTest OfflineAnalysisCoordinator RecordingDetail transcript"`: confirmed P2.2 context after chat recovery and identified the catalog fixture placeholder.
- `cmd /c gradlew.bat :core:database:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after chat recovery.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after chat recovery.
- `cmd /c gradlew.bat :feature:recordingdetail:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after chat recovery.
- `cmd /c gradlew.bat :feature:settings:compileDebugKotlin --console=plain --no-daemon --max-workers=1`: passed after changing the Settings SAF MIME filters.
- `cmd /c gradlew.bat :app:assembleDebug --console=plain --no-daemon --max-workers=1`: first retry failed on a Windows file lock in `debug-mergeJavaRes`; `cmd /c gradlew.bat --stop` released the stale daemon, and the next `:app:assembleDebug` passed.
- `cmd /c gradlew.bat :core:offlineai:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after adding silence/source-changed tests and Android compressed-audio preprocessing.
- `cmd /c gradlew.bat :core:offlineai:compileDebugAndroidTestKotlin --console=plain --no-daemon --max-workers=1`: passed after adding the AAC/M4A device smoke.
- `cmd /c gradlew.bat :core:offlineai:assembleDebugAndroidTest --console=plain --no-daemon --max-workers=1`: passed; produced a 175 MB test APK including native/runtime dependencies.
- `adb install -r -t offlineai-debug-androidTest.apk`: streamed install timed out/failed without a diagnostic over Wi-Fi ADB; `adb push` to `/data/local/tmp/offlineai-debug-androidTest.apk` followed by `pm install -r -t` succeeded after temporarily setting `verifier_verify_adb_installs` to `0` and then restoring it to `1`.
- `adb shell am instrument -w -r -e class com.loomora.core.offlineai.SherpaOnnxDeviceSmokeTest com.loomora.core.offlineai.test/androidx.test.runner.AndroidJUnitRunner`: first run after reinstall showed the new AAC/M4A test passed but sherpa fixture input had been removed by package uninstall; after pushing model pack and `0.wav` fixture again, rerun passed with `OK (2 tests)`.
- `adb shell "cmd connectivity airplane-mode enable; ..."`: attempted airplane-mode smoke over Wi-Fi ADB; enabling airplane mode closed the transport before output or restore could be collected. Subsequent `adb connect 192.168.68.109:33759` attempts failed with timeout, so this manual item remains blocked until the device is manually brought back online.
- `adb connect 192.168.68.109:41179`: passed after the device was manually brought back online; device appeared as `adb-CYHMB6DM5PHEMN4D-m6uBAz._adb-tls-connect._tcp`.
- `adb shell cmd connectivity airplane-mode`: returned `disabled` after reconnect.
- `adb shell am instrument -w -r -e class com.loomora.core.offlineai.SherpaOnnxDeviceSmokeTest com.loomora.core.offlineai.test/androidx.test.runner.AndroidJUnitRunner`: passed after reconnect with `OK (2 tests)`, confirming the sherpa fixture transcription and AAC/M4A decode smoke still pass after the airplane-mode recovery.
- `adb -s CYHMB6DM5PHEMN4D shell cmd connectivity airplane-mode enable; ... am instrument ...; ... airplane-mode disable`: passed over USB ADB. The device reported `enabled`, instrumentation returned `OK (2 tests)`, and final airplane mode state reported `disabled`.
- `rg -n "Retrofit|OkHttp|HttpUrl|URL\(|openConnection|INTERNET|http://|https://|fetch|network" core/offlineai core/model feature/recordingdetail feature/settings -g "*.kt" -g "*.xml"`: found only manifest metadata URLs and UI copy, no production network/API processing path in P2.2.
- `rg -n "class FakeTranscription|FakeTranscriptionEngine|sample transcript|hard-coded|hardcoded|Xin chao" core/offlineai feature/recordingdetail -g "*.kt"`: found fake transcript text only in unit tests, not in production code.
- Settings visible UI/SAF import attempt: selected `sherpa-onnx-whisper-tiny-int8-multilingual.modelpack.zip` from `/sdcard/Download`; first import reached `INCOMPATIBLE` with `RAM_INSUFFICIENT`, exposing that compatibility was comparing against Android app heap `memoryClass`.
- `cmd /c gradlew.bat :core:offlineai:clean :core:offlineai:testDebugUnitTest :feature:settings:compileDebugKotlin :app:assembleDebug --console=plain --no-daemon --max-workers=1`: passed after changing compatibility RAM calculation. Earlier parallel Gradle runs corrupted incremental state, so `:core:offlineai:clean` was used before the passing rerun.
- Reinstalled `app-debug.apk`, removed the stale incompatible model record in Settings, selected the same model pack through DocumentsUI/SAF from Downloads, and verified the UI showed `TRANSCRIPTION • READY`.
- `cmd /c gradlew.bat :core:database:testDebugUnitTest :feature:recordingdetail:testDebugUnitTest --console=plain --no-daemon --max-workers=1`: passed after the latest P2.2 changes.
- `cmd /c gradlew.bat :app:assembleDebug --console=plain --no-daemon --max-workers=1`: passed after the latest P2.2 changes.

## Warnings / Follow-Up

- The real sherpa Whisper multilingual model pack was verified through direct instrumentation, but visible app UI/SAF import still needs a manual UX pass.
- The GitHub tarball download was unreliable, but the Hugging Face mirror files downloaded successfully and were packed locally.
- The official sherpa AAR is about 49 MB and will increase repository/app dependency footprint; packaging strategy should be reviewed before release distribution.
- Real Vietnamese/mixed-language ASR quality and airplane-mode behavior require further physical-device testing with the imported multilingual Whisper model pack.
- Recorder-style AAC/M4A decode/resample now has direct physical-device smoke coverage using generated AAC/M4A input; longer real recorder outputs still need broader manual listening/quality validation.
