# P3 Final Report

Date: 2026-07-29

## 1. Source State

- Branch: `main`.
- Base/current commit before uncommitted P3 work: `4ccf9be2277c4cceba3043951d671970a77164a0`.
- P3 remains uncommitted because no commit was requested.
- Changes span app navigation/theme, shared core modules, user-facing feature modules, tests, resources, Room schema 8, and evidence documents.

## 2. Localization

- English is the fallback; Vietnamese is complete and accented.
- App language uses AppCompat per-app locales and synchronizes with Android system language behavior.
- App language and transcript language are independent typed preferences with legacy migration and safe fallback.
- Feature-owned EN/VI resources cover Onboarding, Home, Recorder, Editor, Library, Recording Detail, Settings, and Subscription.
- Automated resource parity and common unaccented-Vietnamese checks pass.

## 3. Data Migration

- Room database version is 8; migration 7 to 8 adds persisted Offline AI timing JSON without transcript content.
- Legacy `language_code` values `en`, `vi`, and `vi-VN` migrate to typed values; unknown values fall back to English.

## 4. Onboarding And Permissions

- Version 2 is a five-page pager: language, recording guidance, Offline AI workflow, privacy, and optional microphone permission.
- Completion is guarded and persisted; Settings exposes the tutorial without resetting completion.
- Microphone permission is requested at point of use and was verified by creating a physical-device recording.

## 5. UI Result

- Home has the recording hero, four quick modes, active AI state, trial status, and recent-item status.
- Recorder has localized state, authoritative duration, waveform, markers, pause/resume, and guarded stop/save.
- Library has search, storage summary, favorites/trash actions, and recording cards.
- Recording Detail has Overview, Transcript, and Insights tabs, locale-aware date/time, seek/highlight behavior, and processing/cancel states.
- Settings covers appearance, languages, analysis/performance defaults, model metadata/checksum, tutorial, and app version.
- Compact bottom navigation keeps all labels visible; the AppCompat theme prevents the locale-launch crash.
- Onboarding and Settings can download/install the recommended multilingual transcription model with progress; manual package import remains available.
- Recording Detail uses “Offline analysis” and immediately reports a missing or stale model without enqueueing work.
- Recording Detail can rerun the selected mode, edit the insight title/summary locally, and persist the result as a user-edited revision.

## 6. Offline AI Before/After

- Queued work is reconciled against WorkManager terminal state, failed work can be replaced on retry, and the queued label now clearly says that processing is waiting on the device.

- Before: duplicate source hashing, full-PCM transcription, repeated recognizer construction, and transcript availability delayed behind optional stages.
- After: one persisted fingerprint, VAD-window transcription, recognizer caching by model/language/thread profile, incremental transcript publication, and preservation after later-stage failure.

## 7. Performance And Timing

- OPPO CPH2339 Android 12 smoke suite passed Whisper, diarization, AAC streaming decode, and extractive insights in 25.22 seconds total.
- A physical 69-second full-analysis rerun completed in 41.4 seconds (decode 29.6s, transcription 3.7s, diarization 7.7s). The three detected speech turns were aligned without duplicating text or assigning words to silent gaps.
- This is smoke evidence, not the required representative benchmark.
- No 30-second/5-minute EN/VI values are fabricated because approved clips were unavailable.
- Persisted stages cover fingerprint, decode/resample, speech detection, recognizer load, transcription, diarization, alignment, insights, cleanup, and total time; payloads exclude transcript text.

## 8. Cache Behavior

- The Sherpa recognizer cache reuses one matching entry and deterministically releases it when the model, language, or thread profile changes.
- Unit tests cover reuse, replacement, and release.

## 9. Physical QA And Screenshots

- Device: OPPO CPH2339, Android 12, 720 x 1612 pixels, 320 dpi (360 dp width).
- Verified launch, locale persistence, EN/VI, dark mode, recording/save, populated Library, and all Recording Detail tabs.
- Verified a real model download on the OPPO from 8% through importer completion; Settings reported the model as Ready after SHA-256 verification.
- Verified an end-to-end retry of a previously stuck Offline analysis job; it progressed from local preparation to completion and populated both Transcript and Insights.
- Screenshots under `docs/screenshots/p3` include onboarding EN, Home EN/VI, Settings EN/VI/dark, Recorder VI, populated Library VI, and Recording Detail Overview/Transcript/Insights VI.
- Test display overrides were removed; final state is 720 x 1612, 320 dpi, font scale 1.0.

## 10. Automated Verification

- `gradlew testDebugUnitTest lintDebug assembleDebug`: pass, 6m 7s, 1020 tasks.
- `gradlew :app:assembleRelease`: pass, 1m 55s, 707 tasks.
- `gradlew :app:connectedDebugAndroidTest`: pass; app currently has zero instrumentation test classes.
- Offline AI unit suite: 49 tests passed; physical smoke suite: 4/4 passed.
- The expanded Offline AI suite also covers stale catalog upgrades, terminal-job reruns, sparse timestamp fusion, and short-transcript insight quality.
- `git diff --check`: pass with line-ending warnings only.

## 11. Release Artifact

- Release APK builds successfully under `app/build/outputs/apk/release`.
- Signing/packaging and release lint vital tasks pass.

## 12. Known Limitations

- Representative 30-second/5-minute EN/VI benchmark clips were absent, so that matrix remains unmeasured.
- Valid before-change screenshots were unavailable and cannot be reconstructed after implementation.
- Instrumentation infrastructure passes but has zero UI test classes; physical screenshots and manual flows provide current UI evidence.

## 13. Issues Fixed During QA

- AppCompat theme launch crash exposed by real-device locale application.
- Missing compact bottom-navigation label.
- Incorrect Vietnamese Recording Detail date ordering.
- Stale READY model records bypassing the initial availability check; readiness now also requires the installed payload file.

## 14. Owner Action

The QA pass also fixed WorkManager failing to instantiate `OfflineAnalysisWorker`: the AndroidX Hilt assisted factory is now generated, and stale queued jobs surface as failures instead of waiting forever.

- Provide approved representative EN/VI clips if quantified before/after performance acceptance is mandatory.
- Review and commit the P3 work when ready; no commit or push was performed automatically.

## 15. Model Quality Controls

- Settings now distinguishes installed transcription models from the model currently in use.
- Tiny is the stable default; Base int8 is an optional 153.2 MB quality candidate selected explicitly by the user.
- The selected model id is persisted in DataStore, copied into each analysis job, and honored by the coordinator and worker.
- Missing or corrupt files for the selected model stop the job instead of silently falling back to another model.
- Catalog validation includes encoder, decoder, token files, and total displayed model size.
- Exact repeated-phrase hallucinations are collapsed across timestamp segments while raw model text is retained for audit.
- Physical OPPO verification covered Base download, SHA verification, model switching, selected-model execution, full analysis, and cross-segment repetition filtering.
- Final post-filter debug unit/lint/assemble gate passed in 52 seconds (1020 tasks); release assemble and release vital lint passed in 52 seconds (707 tasks).
