# Release Checklist

Date: 2026-07-28

## Gate Commands

| Gate | Command | Status | Evidence |
| --- | --- | --- | --- |
| Clean check | `.\gradlew.bat clean check --console=plain --no-daemon --max-workers=1` | Pass | `BUILD SUCCESSFUL in 15m 20s`; 1442 actionable tasks. |
| Unit tests | `.\gradlew.bat testDebugUnitTest --console=plain --no-daemon --max-workers=1` | Pass | `BUILD SUCCESSFUL in 1m 56s`, 489 actionable tasks. |
| Debug build | `.\gradlew.bat assembleDebug --console=plain --no-daemon --max-workers=1` | Pass | `BUILD SUCCESSFUL in 7m 7s`; `app-debug.apk`, 195,716,923 bytes. |
| Release build | `.\gradlew.bat assembleRelease --console=plain --no-daemon --max-workers=1` | Pass unsigned | `BUILD SUCCESSFUL in 11m 57s`; `app-release-unsigned.apk`, 187,953,411 bytes. |
| Release signing validation | `.\gradlew.bat :app:validateReleaseSigning --console=plain --no-daemon --max-workers=1` | Fail expected in workspace | Missing `LOOMORA_STORE_FILE`, `LOOMORA_STORE_PASSWORD`, `LOOMORA_KEY_ALIAS`, and `LOOMORA_KEY_PASSWORD`; production publish blocked until configured outside repo. |
| Website build | `npm.cmd run build` in `web/` | Pass | Next.js build compiled, type-checked, and generated 20 static pages. |
| Device smoke | `.\gradlew.bat :core:offlineai:connectedDebugAndroidTest --console=plain --no-daemon --max-workers=1` | Pass | OPPO `CPH2339` Android 12: 4 tests, 0 skipped, 0 failed; `BUILD SUCCESSFUL in 2m 30s`. |

## Hardening Decisions

- R8/minify: deferred. `release.isMinifyEnabled = false`; there is not enough release-smoke evidence across Room, Hilt, WorkManager, serialization, sherpa JNI, license parsing, and insight schema.
- Release signing: release does not fall back to debug signing. Production publish must run `validateReleaseSigning` with secrets or local `keystore.properties`.
- Private keys: app contains public license verification key only. No production private Android signing key or license signing key is expected in repo/APK.
- AI processing: current accepted path is on-device transcription, diarization, and extractive/heuristic insights after required models are installed. Deep generative LLM enhancement is not release-available.
- APK audit: unsigned release APK contains native libraries for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`; no `.onnx`, `.litertlm`, `.modelpack`, keystore, private-key, `.jks`, or `.keystore` entries were found.
- Website claim scan: stale remote-AI, over-strong privacy, and speech-enhancement claims were removed from Next pages and legacy `web/index.html`.

## Manual Follow-Up

- Release build smoke on a signed production APK.
- Process-kill/reboot queue lifecycle smoke.
- Low-storage model import/process/export smoke.
- Wider device matrix beyond OPPO `CPH2339`.
- Accessibility/localization visual pass after website/app copy settles.
