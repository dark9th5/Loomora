# Known Issues

Date: 2026-07-27

- Production release signing is configured but not verified with a real production key in this workspace.
- R8/minification remains disabled until a later release-hardening task has enough regression coverage.
- Recorder session lifecycle now uses one service command path, but real device recovery behavior after process death/interruption still needs smoke testing.
- Recorder status/duration updates are persisted from active state changes; long-session database write cadence needs device profiling.
- Notification stop action is wired to the same service action and visible in `dumpsys notification`, but direct shade-action tapping was not completed because the physical device notification list was crowded under UIAutomator.
- Recorder permission and save-failure navigation still lack Compose UI tests; current coverage is unit-level, build/lint, and physical smoke testing.
- P0.5 cleanup has deterministic unit/build proof, but rotate/navigate/background leak behavior still needs instrumentation/manual profiling on a real device.
- P1.1 media validation is covered with fake-validator unit tests; physical process-death/interruption recovery with real partially written `.m4a` files still needs device smoke testing.
- P1.1 `Keep` currently dismisses a recovery diagnostic in the active Library session but does not persist a separate acknowledged state across app restarts.
- Existing Vietnamese resource files contain mojibake from earlier encoding issues; new P0.5 Vietnamese keys were added in ASCII to avoid widening the encoding change.
- P1.2 share/export flows have build and unit coverage, but SAF destination UX, duplicate-destination handling by document providers, and `FileProvider` share interoperability still need physical-device smoke testing.
- P1.2 storage usage currently scans app-local directories only; external user-managed exports outside app storage are intentionally not counted back into usage totals.
- P1.3 waveform extraction is covered with deterministic unit tests, but real-device validation is still needed for long compressed recorder outputs, seek alignment tolerance, and delete-while-generating cancellation.
- P1.4 real audio export now has unit/build coverage, but physical-device listening/validation is still needed for recorder-produced AAC/M4A files, long compressed edits, and OEM-specific codec behavior.
- P1.4 currently rejects speech-clarity export and `Split` export explicitly; those operations need a validated DSP/editor follow-up rather than a fake success path.
- Editor export is now real and non-destructive, but it still needs physical-device listening verification on recorder-produced files.
- Offline transcription persistence and coordinator flow now exist, and the production sherpa-onnx Android runtime is wired through the official `1.13.4` AAR. A physical-device smoke test has verified real Whisper tiny int8 ASR on a deterministic WAV fixture.
- P2.2 production `SherpaOnnxTranscriptionEngine` still fails typed when the required model pack files are absent or invalid; it does not fake transcript success.
- The official multilingual `sherpa-onnx-whisper-tiny.tar.bz2` GitHub tarball timed out/reset in this workspace, but Hugging Face mirror files were downloaded and packed locally under `build/model-fixtures`.
- Visible app UI/SAF import of the generated Whisper tiny int8 model pack still needs manual UX verification, even though the importer and direct device smoke path are covered.
- The official sherpa-onnx AAR is approximately 49 MB, so release packaging size and distribution strategy need review before production.
- Recorder-produced AAC/M4A decode/resample to 16 kHz mono needs Android decoder hardening and real-device testing; current deterministic tests use PCM16 WAV fixtures.
- LiteRT-LM official Android artifacts checked on Monday, July 27, 2026 currently carry newer Kotlin metadata than the repo Kotlin `2.1.0` toolchain can compile against directly, so the dependency is packaged as `runtimeOnly` until the repo toolchain or adapter strategy is advanced.
- Offline model-management UI is functional but still uses hard-coded English strings in this slice and needs localization follow-up.
- Airplane-mode smoke verification with an imported sherpa Whisper multilingual model fixture still needs real-device testing.
- Gradle `connectedDebugAndroidTest` had one stale failure from an earlier `UiAutomation` copy helper and one transient system-abort/ADB instability while the device was recovering; direct Wi-Fi instrumentation later passed with `OK (1 test)`.
- Gradle reports deprecated features that will be incompatible with Gradle 9.0.
- A lint/Kotlin FIR crash was observed on a top-level test fake DAO; the immediate test shape was fixed, but dependency/toolchain upgrades should watch for recurrence.
